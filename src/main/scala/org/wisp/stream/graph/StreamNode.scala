package org.wisp.stream.graph

import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{RunnableSink, RunnableTransformer, SplitStream, StreamBuffer, StreamFlow, StreamSink, StreamTransformer}

import scala.concurrent.ExecutionContextExecutor

/** 
 * Stream element 
 */
class StreamNode[T](graph: StreamGraph, val link: StreamFlow[T]) {
  given ExecutionContextExecutor = graph.system

  /** 
   * Builder for [[org.wisp.stream.iterator.StreamTransformer#map]]
   */
  def map[V](function: T => V): StreamNode[V] = {
    val r = StreamTransformer.map[T, V](link, function)
    graph.apply(r)
  }

  /**
   * Builder for [[org.wisp.stream.iterator.RunnableTransformer#map]]
   */
  def mapTo[V](function: T => V): RunnableTransformer[T, V] = {
    RunnableTransformer.map[T, V](link, function)
  }

  /** 
   * Builder for [[org.wisp.stream.iterator.StreamTransformer#filter]]
   */
  def filter(predicate: T => Boolean): StreamNode[T] = {
    val r = StreamTransformer.filter[T](link, predicate)
    graph.apply(r)
  }

  /**
   * Builder for [[org.wisp.stream.iterator.RunnableTransformer#filter]]
   */
  def filterTo(predicate: T => Boolean): RunnableTransformer[T, T] = {
    RunnableTransformer.filter[T](link, predicate)
  }

  /** 
   * Builder for [[org.wisp.stream.iterator.StreamTransformer#flatMap]]
   */
  def flatMap[V](function: T => Source[V]): StreamNode[V] = {
    val r = StreamTransformer.flatMap[T, V](link, function)
    graph.apply(r)
  }

  /**
   * Builder for [[org.wisp.stream.iterator.RunnableTransformer#flatMap]]
   */
  def flatMapTo[V](function: T => Source[V]): RunnableTransformer[T, V] = {
    RunnableTransformer.flatMap[T, V](link, function)
  }

  /**
   * Builder for [[org.wisp.stream.iterator.StreamTransformer#fold]]
   */
  def fold[V](zero:V)(fold: (V, T) => V): StreamNode[V] = {
    val r = StreamTransformer.fold[T, V](link, zero, fold)
    graph.apply(r)
  }

  /**
   * Builder for [[org.wisp.stream.iterator.RunnableTransformer#fold]]
   */
  def foldTo[V](zero:V)(fold: (V, T) => V): RunnableTransformer[T, V] = {
    RunnableTransformer.fold[T, V](link, zero, fold)
  }

  /**
   * Builder for [[org.wisp.stream.iterator.StreamTransformer]]
   */
  def collect[V](function: Option[T] => Source[V]): StreamNode[V] = {
    val r = StreamTransformer[T, V](link, function)
    graph.apply(r)
  }

  /**
   * Builder for [[org.wisp.stream.iterator.RunnableTransformer]]
   */
  def collectTo[V](function: Option[T] => Source[V]): RunnableTransformer[T, V] = {
    RunnableTransformer[T, V](link, function)
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
   * val source = new StreamGraph(as).from((0 until 5).asSource)
   * source.split{ s =>
   *   s.copy.map(i => i * 2).to(println).start // println (0 2 4 6 8)
   *   s.copy.map(i => i * 2 + 1).to(println).start // println (1 3 5 7 9)
   * }
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
   * `sink` wil be run inside [[org.wisp.stream.iterator.RunnableSink#run]]
   */
  def toRunnable(sink: Sink[T]): RunnableSink[T] = {
    RunnableSink(link ,sink)
  }

  /**
   * [[org.wisp.stream.iterator.StreamBuffer]]
   */
  def buffer(size:Int) : StreamNode[T] = {
    val r = StreamBuffer(link, size)
    graph.apply(r)
  }

  /**
   * provide `this` as argument to `function`
   */
  def as[R](function: this.type => R): R = {
    function.apply(this)
  }

}
