package org.wisp

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, RejectedExecutionException}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.control.NonFatal

class ActorSystem(inboxCapacity:Int = 3, finalizeWith:Option[ExecutionContext] = Some(ExecutionContext.parasitic)) extends ExecutionContext, AutoCloseable{

  val executor: ExecutionContextExecutorService = createExecutor()
  protected def createExecutor() : ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService( Executors.newVirtualThreadPerTaskExecutor(), reportFailure )
  }

  protected val closed: AtomicBoolean = AtomicBoolean(false)

  protected def asRunnable(command: Runnable):Runnable = {
    () => {
      try {
        command.run()
      } catch {
        case NonFatal(ex) =>
          reportFailure(ex)
      }
    }
  }

  override def execute(command: Runnable): Unit = {
    try {
      executor.execute(asRunnable(command))
    }catch{
      case e : RejectedExecutionException =>
        if(closed.get() && finalizeWith.isDefined) {
          finalizeWith.get.execute(asRunnable(command))
        }else{
          throw e
        }
    }
  }

  override def reportFailure(cause: Throwable): Unit = {
    cause.printStackTrace()
  }

  def as[R](fn: ExecutionContext ?=> ActorSystem => R ):R = {
    given ExecutionContext = this
    val f: ActorSystem => R = fn
    f.apply(this)
  }

  def create[T <: Actor](fn: Inbox => T):T = {
    create(inboxCapacity, fn)
  }
  
  def create[T <: Actor](inboxSize:Int, fn: Inbox => T):T = {
    QueueInbox(this, inboxSize, fn).actor
  }

  override def close(): Unit = {
    closed.set(true)
    executor.close()
  }

}
