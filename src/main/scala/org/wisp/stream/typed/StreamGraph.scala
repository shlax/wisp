package org.wisp.stream.typed

import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{ForEachSink, ForEachSource, SourceActorLink, StreamSource, ZipStream}

import scala.concurrent.ExecutionContext

class StreamGraph(val system:ActorSystem){
  given ExecutionContext = system

  def sorce[T](link: SourceActorLink): SourceNode[T] = {
    SourceNode(this, link)
  }

  def node[T](link: ActorLink): StreamNode[T] = {
    StreamNode(this, link)
  }

  def from[T](s:Source[T]) : SourceNode[T] = {
    sorce(StreamSource(s))
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

  def forEach[T, E](s: Source[T])(fn: SourceNode[T] => E): (ForEachSource[T], E) = {
    val f = ForEachSource(s)
    val g = fn.apply(sorce(f))
    (f, g)
  }

  def forEach[T](s:Source[T])(fn : SourceNode[T] => Unit ) : ForEachSource[T] = {
    val f = ForEachSource(s)
    fn.apply( sorce(f) )
    f
  }

  def forEach[T, R](s:Source[T], c:Sink[R])(fn: SourceNode[T] => StreamNode[R]) : ForEachSink[T, R] = {
    ForEachSink(s, c ){ prev =>
      fn.apply( sorce(prev) ).link
    }
  }

}
