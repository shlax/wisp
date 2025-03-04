package org.wisp

import java.util.concurrent.{Executors, RejectedExecutionException}
import scala.concurrent.duration.Duration
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.control.NonFatal

class ActorSystem(inboxCapacity:Int = 3) extends ExecutionContext, AutoCloseable{

  val executor: ExecutionContextExecutorService = createExecutor()
  protected def createExecutor() : ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService( Executors.newVirtualThreadPerTaskExecutor(), reportFailure )
  }

  override def execute(command: Runnable): Unit = {
    try {
      executor.execute(() => {
        try {
          command.run()
        } catch {
          case NonFatal(ex) =>
            reportFailure(ex)
        }
      })
    }catch{
      case _ : RejectedExecutionException =>
        command.run()
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
    executor.close()
  }

}
