package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.utils.lock.*
import org.wisp.stream.Sink

import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContext

/**
 * This class implements a stream sink that can be executed on a thread to consume elements from an upstream actor.
 *
 * Execution context for async operations is provided by the `ExecutionContext` parameter.
 *
 * @tparam T the type of elements consumed by this sink
 *
 * @param upstream         the upstream link providing elements
 * @param sink             the underlying sink implementation that processes elements
 */
class RunnableSink[T](upstream:ActorLink, override val sink:Sink[T])(using ExecutionContext) extends StreamActorLink, RunnableStream, SinkExecution[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()

  /** [[java.util.concurrent.locks.Condition]] used to coordinate synchronization between the processing thread and message handler */
  protected val condition: Condition = lock.newCondition()

  protected var value: Option[T] = None

  protected var started: Boolean = false
  protected var ended = false

  /**
   * Requests the next element from the upstream actor.
   */
  protected def next(): Unit = {
    upstream.call(HasNext).onComplete(apply)
  }

  /** Stores any exception thrown by the sink during element processing */
  protected var sinkException: Option[Throwable] = None

  protected override def onSinkException(t: Throwable): Unit = {
    sinkException = Some(t)
  }

  /**
   * Executes the sink processing loop on the calling thread. This method blocks until the stream ends.
   * After processing all elements, completes the sink and rethrows last exception that occurred during processing.
   */
  override def run(): Unit = lock.withLock {
    if (started) {
      throw new IllegalStateException("started")
    } else {
      started = true
    }

    next()

    while (!ended) {

      for (v <- value) {
        value = None
        tryApply(v)
        next()
      }

      if (!ended) {
        condition.await()
      }

    }

    sink.complete()
    
    if (sinkException.isDefined) {
      throw sinkException.get
    }

  }

  /**
   * Handles messages from the upstream actor.
   */
  override def apply(from: ActorLink): PartialFunction[Operation, Unit] = {
    case Next(v) =>
      if(ended) throw new IllegalStateException("ended")
      if(value.isDefined) throw new IllegalStateException("dropped value: "+v)

      value = Some(v.asInstanceOf[T])
      condition.signal()
    case End =>
      if(ended) throw new IllegalStateException("ended")
      
      ended = true
      condition.signal()
  }

}
