package org.wisp

import java.util.concurrent.{Executor, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.control.NonFatal

class ActorSystem(inboxCapacity:Int = 3) extends ExecutionContext, AutoCloseable{

  val executor: ExecutionContextExecutorService = createExecutorService()
  protected def createExecutorService() : ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService( Executors.newVirtualThreadPerTaskExecutor() )
  }

  override def execute(command: Runnable): Unit = {
    executor.execute(() => {
      try{
        command.run()
      }catch {
        case NonFatal(e) =>
          reportFailure(e)
      }
    })
  }

  override def reportFailure(cause: Throwable): Unit = {
    cause.printStackTrace()
  }

  def create[T <: Actor](fn: ActorFactory[T], inboxSize:Int = inboxCapacity):T = {
    QueueInbox(this, inboxSize, fn).actor
  }

  override def close(): Unit = {
    executor.close()
  }

}
