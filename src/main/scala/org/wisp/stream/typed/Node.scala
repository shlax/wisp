package org.wisp.stream.typed

import org.wisp.ActorLink
import org.wisp.stream.iterator.{StreamWorker, StreamSink, StreamBuffer}

import java.util.function.Consumer

class Node[T](val graph: Graph, val link: ActorLink) {

  def map[V](fn: T => V): Node[V] = {
    val r = graph.system.create( i => StreamWorker(link, i, (a: Any) => fn.apply(a.asInstanceOf[T]) ) )
    graph.node(r)
  }

  def to[E >: T](c: Consumer[E]): StreamSink = {
    StreamSink(graph.system, link, (a: Any) => c.accept(a.asInstanceOf[T]) )
  }

  def buffer(size:Int) : Node[T] = {
    val r = StreamBuffer(graph.system ,link, size)
    graph.node(r)
  }

  def as[R](fn: Node[T] => R): R = {
    fn.apply(this)
  }

}