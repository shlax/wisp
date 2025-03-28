package org.wisp.stream.typed

import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{ForEachSink, ForEachSource, SourceActorLink, StreamSource, ZipStream}

import scala.concurrent.ExecutionContext

/** Api for creating stream graphs. */
class StreamGraph(val system:ActorSystem){
  given ExecutionContext = system

  /** Convert [[ActorLink]] to [[StreamNode]] */
  def node[T](link: ActorLink): StreamNode[T] = {
    StreamNode(this, link)
  }

  /** Create stream [[SourceNode]] from [[SourceActorLink]] */
  def from[T](link: SourceActorLink): SourceNode[T] = {
    SourceNode(this, link)
  }

  /** Create stream [[SourceNode]] from [[Source]] */
  def from[T](s:Source[T]) : SourceNode[T] = {
    from(StreamSource(s))
  }

  /** Combine multiple `streams` into one  */
  def zip[T](streams: Iterable[StreamNode[? <: T]]): StreamNode[T] = {
    val r = ZipStream(streams.map(_.link))
    node(r)
  }

  /** Combine multiple `streams` into one  */
  def zip[T](streams:StreamNode[? <: T]*): StreamNode[T] = {
    zip(streams)
  }

  def forEach[T, R](s:Source[T])(fn : SourceNode[T] => R ) : (ForEachSource[T], R) = {
    val f = ForEachSource(s)
    val r = fn.apply(from(f))
    (f, r)
  }

  def forEach[T, R](s:Source[T], c:Sink[R])(fn: SourceNode[T] => StreamNode[R]) : ForEachSink[T, R] = {
    ForEachSink(s, c ){ prev =>
      fn.apply(from(prev)).link
    }
  }

}
