package org.wisp.stream.typed

import org.wisp.ActorLink
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{RunnableSink, SplitStream, StreamBuffer, StreamSink, StreamWorker}

import scala.concurrent.ExecutionContext

/** 
 * Stream element 
 */
class StreamNode[T](graph: StreamGraph, val link: ActorLink) {
  given ExecutionContext = graph.system

  /** 
   * [[org.wisp.stream.iterator.StreamWorker#map]]
   */
  def map[V](fn: T => V): StreamNode[V] = {
    val r = graph.system.create( i => StreamWorker.map(link, i, fn) )
    graph.node(r)
  }

  /** 
   * [[org.wisp.stream.iterator.StreamWorker#filter]]
   */
  def filter(fn: T => Boolean): StreamNode[T] = {
    val r = graph.system.create(i => StreamWorker.filter(link, i, fn))
    graph.node(r)
  }

  /** 
   * [[org.wisp.stream.iterator.StreamWorker#flatMap]]
   */
  def flatMap[V](fn: T => Source[V]): StreamNode[V] = {
    val r = graph.system.create(i => StreamWorker.flatMap(link, i, fn) )
    graph.node(r)
  }

  class SplitNode(from: SplitStream#Split) {
    /**
     * Create new copy
     */
    def copy: StreamNode[T] = StreamNode[T](graph, from.copy)
  }

  /**
   * Duplicate current stream
   * [[org.wisp.stream.iterator.StreamWorker]]
   */
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

  /**
   * `Sink` wil be run inside [[org.wisp.stream.iterator.RunnableSourceSink#run]]
   */
  def toRunnable[E >: T](c: Sink[E]): RunnableSink[E] = {
    RunnableSink[E](link ,c)
  }

  /**
   * [[org.wisp.stream.iterator.RunnableSourceSink]]
   */
  def buffer(size:Int) : StreamNode[T] = {
    val r = StreamBuffer(link, size)
    graph.node(r)
  }

  def as[R](fn: this.type => R): R = {
    fn.apply(this)
  }

}
