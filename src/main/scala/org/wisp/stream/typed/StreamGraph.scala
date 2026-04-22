package org.wisp.stream.typed

import org.wisp.Link
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{RunnableSourceSink, RunnableSource, SourceLink, StreamSource, ZipStream}
import org.wisp.stream.iterator.Operation
import scala.concurrent.ExecutionContext

/**
 * Api for creating stream graphs.
 */
class StreamGraph(val system:ExecutionContext){
  given ExecutionContext = system

  /**
   * Create node from `link`
   */
  def node[T](link: Link[Operation[T], Operation[T]]): StreamNode[T] = {
    StreamNode(this, link)
  }

  /**
   * Create stream from `link`
   */
  def from[T](link: SourceLink[T]): SourceNode[T] = {
    SourceNode(this, link)
  }

  /**
   * Create stream from `source` ussing [[org.wisp.stream.iterator.StreamSource]]
   */
  def from[T](source:Source[T]) : SourceNode[T] = {
    from(StreamSource(source))
  }

  /**
   * Combine multiple `streams` into one using [[org.wisp.stream.iterator.ZipStream]]
   */
  def zip[T](streams: Iterable[StreamNode[T]]): StreamNode[T] = {
    val r = ZipStream[T]( streams.map(_.link) )
    node(r)
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
   * `source` wil be run inside [[org.wisp.stream.iterator.RunnableSource#run]]
   */
  def fromRunnable[T, R](source:Source[T])(fn : SourceNode[T] => Unit ) : RunnableSource[T] = {
    val f = RunnableSource(source)
    fn.apply(from(f))
    f
  }

  /**
   * `source` and `sink` wil be run inside [[org.wisp.stream.iterator.RunnableSourceSink#run]]
   */
  def runnable[T, R](source:Source[T], sink:Sink[R])(fn: SourceNode[T] => StreamNode[R]) : RunnableSourceSink[T, R] = {
    RunnableSourceSink(source, sink ){ prev =>
      fn.apply(from(prev)).link
    }
  }

}
