package org.wisp.stream.graph

import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.{RunnableSource, RunnableSourceSink, StreamFlow}

import scala.concurrent.ExecutionContextExecutor

object dsl {

  /**
   * Alias for [[StreamGraph#wrapNode]]
   */
  def wrapNode[T](link: StreamFlow[T])(using ExecutionContextExecutor): StreamNode[T] = {
    val g = new StreamGraph()
    g.wrapNode(link)
  }

  /**
   * Alias for [[StreamGraph#fromSource]]
   */
  def fromSource[T](source: Source[T])(using ExecutionContextExecutor): StreamNode[T] = {
    val g = new StreamGraph()
    g.fromSource(source)
  }

  /**
   * Alias for [[StreamGraph#fromRunnable]]
   */
  def fromRunnable[T, R](source: Source[T])(fn: StreamNode[T] => Unit)(using ExecutionContextExecutor): RunnableSource[T] = {
    val g = new StreamGraph()
    g.fromRunnable(source)(fn)
  }

  /**
   * Alias for [[StreamGraph#runnable]]
   */
  def runnable[T, R](source: Source[T], sink: Sink[R])(fn: StreamNode[T] => StreamNode[R])(using ExecutionContextExecutor): RunnableSourceSink[T, R] = {
    val g = new StreamGraph()
    g.runnable(source, sink)(fn)
  }

  /**
   * Alias for [[StreamGraph#zipNodes]]
   */
  def zipNodes[T](streams: Iterable[StreamNode[T]])(using ExecutionContextExecutor): StreamNode[T] = {
    val g = new StreamGraph()
    g.zipNodes(streams)
  }

  /**
   * Alias for [[StreamGraph#zipNodes]]
   */
  def zipStreams[T](streams: Iterable[StreamFlow[T]])(using ExecutionContextExecutor): StreamNode[T] = {
    val g = new StreamGraph()
    g.zipStreams(streams)
  }

  /**
   * Alias for [[StreamGraph#zipNodes]]
   */
  def zipNodes[T](streams: StreamNode[T]*)(using ExecutionContextExecutor): StreamNode[T] = {
    val g = new StreamGraph()
    g.zipNodes(streams)
  }

  /**
   * Alias for [[StreamGraph#zipStreams]]
   */
  def zipStreams[T](streams: StreamFlow[T]*)(using ExecutionContextExecutor): StreamNode[T] = {
    val g = new StreamGraph()
    g.zipStreams(streams)
  }

}
