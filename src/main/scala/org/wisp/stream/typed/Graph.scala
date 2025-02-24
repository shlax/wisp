package org.wisp.stream.typed

import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.Source
import org.wisp.stream.iterator.{StreamSource, ForEachSink, ForEachSource, ZipStream}

import java.util.function.Consumer

class Graph(val system:ActorSystem){

  def node[T](link: ActorLink): Node[T] = {
    Node(this, link)
  }

  def from[T](s:Source[T]) : Node[T] = {
    node(StreamSource(s))
  }

  def zip[T](nodes: Iterable[Node[T]]): Node[T] = {
    val r = ZipStream(system, nodes.map(_.link))
    node(r)
  }

  def zip[T](nodes:Node[T]*): Node[T] = {
    zip(nodes)
  }

  def forEach[T](s:Source[T])(fn : Node[T] => Unit ) : ForEachSource = {
    val f = ForEachSource(s)
    fn.apply( node(f) )
    f
  }

  def forEach[T, R](s:Source[T], c:Consumer[R])(fn: Node[T] => Node[R]) : ForEachSink = {
    ForEachSink(system, s, (a: Any) => c.accept(a.asInstanceOf[R]) ){ prev =>
      fn.apply( node(prev) ).link
    }
  }

}
