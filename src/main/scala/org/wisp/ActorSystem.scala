package org.wisp

import java.util.concurrent.{Executor, ExecutorService, Executors}
import scala.util.control.NonFatal

class ActorSystem(val inboxCapacity:Int = 3) extends Executor, AutoCloseable{

  val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
  
  override def execute(command: Runnable): Unit = {
    executor.execute(() => {
      try{
        command.run()
      }catch {
        case NonFatal(e) =>
          e.printStackTrace()
      }
    })
  }

  def create[T <: Actor](fn: ActorCreator[T], inboxCapacity:Int = inboxCapacity):T = {
    QueueInbox(this, inboxCapacity, fn).actor
  }

  def handle(message: Message, actor: Option[Actor] = None, e: Option[Throwable] = None): Unit = {
    new RuntimeException("error processing "+message+" in "+actor, e.orNull).printStackTrace()
  }

  override def close(): Unit = {
    executor.close()
  }

}
