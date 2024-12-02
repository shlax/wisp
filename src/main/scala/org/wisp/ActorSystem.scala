package org.wisp

import java.util.concurrent.{Executor, ExecutorService, Executors}

class ActorSystem(val inboxCapacity:Int) extends Executor, AutoCloseable{

  val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
  
  override def execute(command: Runnable): Unit = {
    executor.execute(command)
  }

  def create(fn: ActorCreator, inboxCapacity:Int = inboxCapacity):ActorRef = {
    new QueueInbox(this, inboxCapacity, fn).actor
  }

  def handle(message: Message, actor: Option[Actor] = None, e: Option[Throwable] = None): Unit = { /* ? */ }

  override def close(): Unit = {
    executor.shutdown()
  }

}
