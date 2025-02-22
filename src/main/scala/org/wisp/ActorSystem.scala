package org.wisp

import org.wisp.exceptions.ExceptionHandler

import java.util.concurrent.{Executor, ExecutorService, Executors}
import scala.util.control.NonFatal

class ActorSystem(val inboxCapacity:Int = 3) extends Executor, ExceptionHandler, AutoCloseable{

  val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
  
  override def execute(command: Runnable): Unit = {
    executor.execute(() => {
      try{
        command.run()
      }catch {
        case NonFatal(e) =>
          handle(e)
      }
    })
  }

  def create[T <: Actor](fn: ActorCreator[T], inboxCapacity:Int = inboxCapacity):T = {
    QueueInbox(this, inboxCapacity, fn).actor
  }

  override def close(): Unit = {
    executor.close()
  }

}
