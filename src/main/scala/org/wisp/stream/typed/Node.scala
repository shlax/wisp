package org.wisp.stream.typed

import org.wisp.ActorLink
import org.wisp.stream.Source
import org.wisp.stream.iterator.{StreamBuffer, StreamSink, StreamWorker}

import java.util.function.Consumer

class Node[T](val graph: Graph, val link: ActorLink) {

  def map[V](fn: T => V): Node[V] = {
    val r = graph.system.create( i => StreamWorker.map(link, i, fn) )
    graph.node(r)
  }

  def flatMap[V](fn: T => Source[V]): Node[V] = {
    val r = graph.system.create(i => StreamWorker.flatMap(link, i, fn))
    graph.node(r)
  }

  def to[E >: T](c: Consumer[E]): StreamSink[E] = {
    StreamSink(link, c)
  }

  def buffer(size:Int) : Node[T] = {
    val r = StreamBuffer(link, size)
    graph.node(r)
  }

  def as[R](fn: Node[T] => R): R = {
    fn.apply(this)
  }

}