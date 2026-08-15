package org.wisp.stream.iterator

import org.wisp.utils.lock.*
import org.wisp.stream.Sink

import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContextExecutor

/**
 * This class implements a stream sink that can be executed on a thread to consume elements from an upstream link.
 *
 * Execution context for async operations is provided by the `ExecutionContextExecutor` parameter.
 *
 * @tparam T the type of elements consumed by this sink
 *
 * @param upstream         the upstream link providing elements
 * @param sink             the underlying sink implementation that processes elements
 */
class RunnableSink[T](upstream:StreamFlow[T], override val sink:Sink[T])(using ec: ExecutionContextExecutor) extends StreamHandler[T], RunnableStream[T], SinkExecution[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()

  /**
   * [[java.util.concurrent.locks.Condition]] used to coordinate synchronization between the processing thread and message handler
   */
  protected val condition: Condition = lock.newCondition()

  protected var value: Option[T] = None

  protected var started: Boolean = false
  protected var ended = false

  /**
   * Requests the next element from the upstream link.
   */
  protected def next(): Unit = {
    upstream.next(this)
  }

  /**
   * Stores any exception thrown by the sink during element processing
   */
  protected var sinkException: Option[Throwable] = None

  protected override def onSinkException(t: Throwable): Unit = {
    lock.withLock{ sinkException = Some(t) }
    ec.reportFailure(t)
  }

  /**
   * Executes the sink processing loop on the calling thread. This method blocks until the stream ends.
   * After processing all elements, completes the sink and rethrows last exception that occurred during processing.
   */
  override def run(): Unit = {

    lock.withLock{
      if (started) {
        throw new IllegalStateException("started")
      } else {
        started = true
      }
    }

    next()

    var end: Boolean = lock.withLock(ended)

    while (!end) {

      val actValue:Option[T] = lock.withLock {
        val tmp = value
        value = None
        tmp
      }

      for (v <- actValue) {
        tryApply(v)
        next()
      }

      lock.withLock {
        if (!ended) {
          condition.await()
        }

        end = ended
      }

    }

    sink.complete()

    lock.withLock {
      if (sinkException.isDefined) {
        throw sinkException.get
      }
    }

  }

  /**
   * Handles messages from the upstream link.
   */
  override def applyWithLock(rv: Response[T]): Unit = rv match {
    case Next(v) =>
      if(ended) throw new IllegalStateException("ended")
      if(value.isDefined) throw new IllegalStateException("dropped value: "+rv)

      value = Some(v)
      condition.signal()
    case End =>
      if(ended) throw new IllegalStateException("ended")
      
      ended = true
      condition.signal()
  }

}
