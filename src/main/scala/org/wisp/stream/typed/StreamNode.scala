package org.wisp.stream.typed

import org.wisp.ActorLink
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{StreamBuffer, StreamSink, StreamWorker}

import scala.concurrent.ExecutionContext

class StreamNode[T](graph: StreamGraph, val link: ActorLink)(using executor: ExecutionContext) extends StreamGraph(graph.system){

  def map[V](fn: T => V): StreamNode[V] = {
    val r = graph.system.create( i => StreamWorker.map(link, i, fn) )
    graph.node(r)
  }

  def flatMap[V](fn: T => Source[V]): StreamNode[V] = {
    val r = graph.system.create(i => StreamWorker.flatMap(link, i, fn) )
    graph.node(r)
  }

  def to[E >: T](c: Sink[E]): StreamSink[E] = {
    StreamSink(link, c)
  }

  def buffer(size:Int) : StreamNode[T] = {
    val r = StreamBuffer(link, size)
    graph.node(r)
  }

  def as[R](fn: StreamNode[T] => R): R = {
    fn.apply(this)
  }

}