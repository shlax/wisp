package org.wisp.stream.typed

import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{ForEachSink, ForEachSource, StreamSource, ZipStream}

class StreamGraph(val system:ActorSystem){

  def node[T](link: ActorLink): StreamNode[T] = {
    StreamNode(this, link)
  }

  def from[T](s:Source[T]) : StreamNode[T] = {
    node(StreamSource(s))
  }

  def zip[T](nodes: Iterable[StreamNode[? <: T]]): StreamNode[T] = {
    val r = ZipStream(nodes.map(_.link))
    node(r)
  }

  def zip[T](nodes:StreamNode[? <: T]*): StreamNode[T] = {
    zip(nodes)
  }

  def forEach[T](s:Source[T])(fn : StreamNode[T] => Unit ) : ForEachSource[T] = {
    val f = ForEachSource(s)
    fn.apply( node(f) )
    f
  }

  def forEach[T, R](s:Source[T], c:Sink[R])(fn: StreamNode[T] => StreamNode[R]) : ForEachSink[T, R] = {
    ForEachSink(s, c ){ prev =>
      fn.apply( node(prev) ).link
    }
  }

}
