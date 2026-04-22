package org.wisp.stream.typed

import org.wisp.ActorLink
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{Operation, RunnableSink, SplitStream, StreamBuffer, StreamSink, StreamWorker}

import scala.concurrent.ExecutionContext

/** 
 * Stream element 
 */
class StreamNode[T](graph: StreamGraph, val link: ActorLink[Operation[T]]) {
  given ExecutionContext = graph.system

  /** 
   * builder for [[org.wisp.stream.iterator.StreamWorker#map]]
   */
  def map[V](fn: T => V): StreamNode[V] = {
    val r = StreamWorker.map[T, V](link, fn)
    graph.node(r)
  }

  /** 
   * builder for [[org.wisp.stream.iterator.StreamWorker#filter]]
   */
  def filter(fn: T => Boolean): StreamNode[T] = {
    val r = StreamWorker.filter[T](link, fn)
    graph.node(r)
  }

  /** 
   * builder for [[org.wisp.stream.iterator.StreamWorker#flatMap]]
   */
  def flatMap[V](fn: T => Source[V]): StreamNode[V] = {
    val r = StreamWorker.flatMap[T, V](link, fn)
    graph.node(r)
  }

  class SplitNode(from: SplitStream[T]#Split) {
    /**
     * Create new copy
     */
    def copy: StreamNode[T] = StreamNode[T](graph, from.copy)
  }

  /**
   * Duplicate current stream using [[org.wisp.stream.iterator.SplitStream]]
   *
   * {{{
   *   val source = new StreamGraph(as).from((0 until 5).asSource)
   *   source.split{ s =>
   *     s.copy.map(i => i * 2).to(println).start // println (0 2 4 6 8)
   *     s.copy.map(i => i * 2 + 1).to(println).start // println (1 3 5 7 9)
   *   }
   * }}}
   */
  def split[E](fn: SplitNode => E): E = {
    var res: Option[E] = None
    SplitStream(link){ s =>
      res = Some( fn.apply(SplitNode(s)) )
    }
    res.get
  }

  def to(c: Sink[T]): StreamSink[T] = {
    StreamSink(link, c)
  }

  /**
   * `sink` wil be run inside [[org.wisp.stream.iterator.RunnableSourceSink#run]]
   */
  def toRunnable(sink: Sink[T]): RunnableSink[T] = {
    RunnableSink(link ,sink)
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
