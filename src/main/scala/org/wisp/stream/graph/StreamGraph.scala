package org.wisp.stream.graph

import org.wisp.stream.Source
import org.wisp.stream.iterator.{RunnableSource, RunnableSourceSink, StreamFlow, StreamSource, ZipStream}

import scala.annotation.targetName
import scala.concurrent.ExecutionContextExecutor

/**
 * Api for creating stream graphs.
 */
class StreamGraph(using val system:ExecutionContextExecutor){

  /**
   * Create node from `link`
   */
  def apply[T](link: StreamFlow[T]): StreamNode[T] = {
    StreamNode(this, link)
  }

  /**
   * Create stream from `source` ussing [[org.wisp.stream.iterator.StreamSource]]
   */
  def from[T](source:Source[T]) : StreamNode[T] = {
    apply(StreamSource(source))
  }

  /**
   * Combine multiple `streams` into one using [[org.wisp.stream.iterator.ZipStream]]
   */
  def zip[T](streams: Iterable[StreamNode[T]]): StreamNode[T] = {
    zip(streams.map(_.link))
  }

  /**
   * Combine multiple `streams` into one using [[org.wisp.stream.iterator.ZipStream]]
   */
  @targetName("zipStreams")
  def zip[T](streams: Iterable[StreamFlow[T]]): StreamNode[T] = {
    val r = ZipStream[T]( streams )
    apply(r)
  }

  /**
   * Combine multiple `streams` into one using [[org.wisp.stream.iterator.ZipStream]]
   *
   * {{{
   *   val graph = new StreamGraph(???)
   *   val source1 = graph.from( (0 until 5).asSource.map(i => i * 2) )
   *   val source2 = graph.from( (0 until 5).asSource.map(i => i * 2 + 1) )
   *   graph.zip(source1, source2).to(println).start // println (0 until 10)
   * }}}
   */
  def zip[T](streams:StreamNode[T]*): StreamNode[T] = {
    zip(streams)
  }

  /**
   * Combine multiple `streams` into one using [[org.wisp.stream.iterator.ZipStream]]
   */
  @targetName("zipStreams")
  def zip[T](streams:StreamFlow[T]*): StreamNode[T] = {
    zip(streams)
  }

  /**
   * `source` wil be run inside [[org.wisp.stream.iterator.RunnableSource#run]]
   */
  def fromRunnable[T, R](source:Source[T])(fn : StreamNode[T] => Unit ) : RunnableSource[T] = {
    val f = RunnableSource(source)
    fn.apply(apply(f))
    f
  }

  /**
   * `source` and `sink` wil be run inside [[org.wisp.stream.iterator.RunnableSourceSink#run]]
   */
  def runnable[T, R](source:Source[T], sink:Option[R] => Unit)(fn: StreamNode[T] => StreamNode[R]) : RunnableSourceSink[T, R] = {
    RunnableSourceSink(source, sink){ prev =>
      fn.apply(apply(prev)).link
    }
  }

}
