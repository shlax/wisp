package org.wisp.stream.typed

import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{RunnableSourceSink, RunnableSource, SourceActorLink, StreamSource, ZipStream}

import scala.concurrent.ExecutionContext

/**
 * Api for creating stream graphs.
 */
class StreamGraph(val system:ActorSystem){
  given ExecutionContext = system

  /**
   * Convert [[org.wisp.ActorLink]] to [[org.wisp.stream.typed.StreamNode]]
   */
  def node[T](link: ActorLink): StreamNode[T] = {
    StreamNode(this, link)
  }

  /**
   * Create stream [[org.wisp.stream.typed.SourceNode]] from [[org.wisp.stream.iterator.SourceActorLink]]
   */
  def from[T](link: SourceActorLink): SourceNode[T] = {
    SourceNode(this, link)
  }

  /**
   * Create stream [[org.wisp.stream.typed.SourceNode]] from [[org.wisp.stream.Source]]
   */
  def from[T](s:Source[T]) : SourceNode[T] = {
    from(StreamSource(s))
  }

  /**
   * Combine multiple `streams` into one
   */
  def zip[T](streams: Iterable[StreamNode[? <: T]]): StreamNode[T] = {
    val r = ZipStream(streams.map(_.link))
    node(r)
  }

  /**
   * Combine multiple `streams` into one
   */
  def zip[T](streams:StreamNode[? <: T]*): StreamNode[T] = {
    zip(streams)
  }

  /**
   * `source` wil be run inside [[org.wisp.stream.iterator.RunnableSource#run]]
   */
  def fromRunnable[T, R](source:Source[T])(fn : SourceNode[T] => R ) : (source:RunnableSource[T], value:R) = {
    val f = RunnableSource(source)
    val r = fn.apply(from(f))
    (f, r)
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
