package org.wisp

import org.wisp.utils.lock.withLock

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{Executors, RejectedExecutionException}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.control.NonFatal

/**
 * Hold [[scala.concurrent.ExecutionContext]] for actors
 *
 * @param inboxCapacity default size for inboxes
 * @param executionContext where the calls to [[execute]] are redirected
 */
class ActorSystem(inboxCapacity:Int = 3, executionContext:Option[ExecutionContextExecutorService] = None) extends ExecutionContext, AutoCloseable{

  protected val executor: ExecutionContextExecutorService = createExecutor()

  /** creates virtual thread executor */
  protected def createExecutor() : ExecutionContextExecutorService = {
    executionContext  match {
      case Some(e) => e
      case None =>
        ExecutionContext.fromExecutorService( Executors.newVirtualThreadPerTaskExecutor(), reportFailure )
    }
  }

  protected val closed: AtomicBoolean = AtomicBoolean(false)

  protected val lock:ReentrantLock = new ReentrantLock()
  protected var finalizeWith:Option[ExecutionContext] = None

  protected def createFinalizeWith() : ExecutionContext = {
    ExecutionContext.parasitic
  }

  override def execute(command: Runnable): Unit = {
    try {
      executor.execute(command)
    }catch{
      case e : RejectedExecutionException =>
        if(closed.get()) {
          val ex = lock.withLock {
            finalizeWith match {
              case Some(ec) => ec
              case None =>
                val x = createFinalizeWith()
                finalizeWith = Some(x)
                x
            }
          }
          ex.execute(command)
        }else{
          throw e
        }
    }
  }


  /**
   * If a custom execution context was provided, delegates failure to it.
   * Otherwise, prints the stack trace to standard error.
   */
  override def reportFailure(cause: Throwable): Unit = {
    executionContext match {
      case Some(ec) =>
        ec.reportFailure(cause)
      case None =>
        cause.printStackTrace()
    }
  }

  /** Create new [[Actor]] with [[inboxCapacity]] queue size */
  def create[T <: Actor](fn: ActorScheduler => T):T = {
    create(inboxCapacity, fn)
  }

  /** Create new [[Actor]] with `inboxSize` queue size */
  def create[T <: Actor](inboxSize:Int, fn: ActorScheduler => T):T = {
    given ExecutionContext = this
    QueueScheduler(inboxSize, fn).actor
  }

  override def close(): Unit = {
    closed.set(true)
    if(executionContext.isEmpty) {
      executor.close()
    }
  }

}
