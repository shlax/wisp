package org.wisp

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.control.NonFatal

class ActorSystem(inboxCapacity:Int = 3) extends ExecutionContext, AutoCloseable{

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
          throw ex
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

  def create[T <: Actor](fn: ActorFactory[T], inboxSize:Int = inboxCapacity):T = {
    QueueInbox(this, inboxSize, fn).actor
  }

  override def close(): Unit = {
    executor.close()
  }

}
