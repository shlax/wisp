package org.wisp

import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.control.NonFatal

class ActorSystem(inboxCapacity:Int = 3, waitToClose:Duration = 10.milli) extends ExecutionContext, AutoCloseable{

  val executor: ExecutionContextExecutorService = createExecutor()
  protected def createExecutor() : ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService( Executors.newVirtualThreadPerTaskExecutor(), reportFailure )
  }

  override def execute(command: Runnable): Unit = {
    executor.execute(() => {
      try{
        command.run()
      }catch {
        case NonFatal(ex) =>
          reportFailure(ex)
      }
    })
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
    Thread.sleep(waitToClose.toMillis)
    executor.close()
  }

}
