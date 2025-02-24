package org.wisp.stream.typed

import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.Source
import org.wisp.stream.iterator.{ActorSource, ForEachSink, ForEachSource, MessageRouter}

import java.util.function.Consumer

class Graph(val system:ActorSystem){

  def node[T](link: ActorLink): Node[T] = {
    Node(this, link)
  }

  def from[T](s:Source[T]) : Node[T] = {
    node(ActorSource(s))
  }

  def router[T](nodes: Iterable[Node[T]]): Node[T] = {
    val r = MessageRouter(nodes.map(_.link))
    node(r)
  }

  def router[T](nodes:Node[T]*): Node[T] = {
    router(nodes)
  }

  def forEach[T](s:Source[T])(fn : Node[T] => Unit ) : ForEachSource = {
    val f = ForEachSource(s)
    fn.apply( node(f) )
    f
  }

  def forEach[T, R](s:Source[T], c:Consumer[R])(fn: Node[T] => Node[R]) : ForEachSink = {
    ForEachSink(s, (a: Any) => c.accept(a.asInstanceOf[R]) ){ prev =>
      fn.apply( node(prev) ).link
    }
  }

}
