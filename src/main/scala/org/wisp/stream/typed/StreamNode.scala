package org.wisp.stream.typed

import org.wisp.ActorLink
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{RunnableSink, RunnableSourceSink, SplitStream, StreamBuffer, StreamSink, StreamWorker}

import scala.concurrent.ExecutionContext

/** Stream element */
class StreamNode[T](graph: StreamGraph, val link: ActorLink) {
  given ExecutionContext = graph.system

  /** @see [[StreamWorker]] `map` */
  def map[V](fn: T => V): StreamNode[V] = {
    val r = graph.system.create( i => StreamWorker.map(link, i, fn) )
    graph.node(r)
  }

  /** @see [[StreamWorker]] `filter` */
  def filter(fn: T => Boolean): StreamNode[T] = {
    val r = graph.system.create(i => StreamWorker.filter(link, i, fn))
    graph.node(r)
  }

  /** @see [[StreamWorker]] `flatMap` */
  def flatMap[V](fn: T => Source[V]): StreamNode[V] = {
    val r = graph.system.create(i => StreamWorker.flatMap(link, i, fn) )
    graph.node(r)
  }

  class SplitNode(from: SplitStream#Split) {
    /** Create new copy */
    def next(): StreamNode[T] = StreamNode[T](graph, from.next())
  }

  /** Duplicate current stream
   * @note elements are not duplicated
   * @see [[SplitStream]] */
  def split[E](fn: SplitNode => E): E = {
    var res: Option[E] = None
    SplitStream(link){ s =>
      res = Some( fn.apply(SplitNode(s)) )
    }
    res.get
  }

  def to[E >: T](c: Sink[E]): StreamSink[E] = {
    StreamSink(link, c)
  }

  /** `Sink` wil be run inside [[RunnableSink.run]] */
  def toRunnable[E >: T](c: Sink[E]): RunnableSink[E] = {
    RunnableSink[E](link ,c)
  }

  /** @see [[StreamBuffer]] */
  def buffer(size:Int) : StreamNode[T] = {
    val r = StreamBuffer(link, size)
    graph.node(r)
  }

  def as[R](fn: this.type => R): R = {
    fn.apply(this)
  }

}