package org.wisp

import java.util.concurrent.{Executor, ExecutorService, Executors}

class ActorSystem(val inboxCapacity:Int) extends Executor {

  private val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
  
  override def execute(command: Runnable): Unit = {
    executor.execute(command)
  }

  def create(fn: ActorCreator, inboxCapacity:Int = inboxCapacity):ActorRef = {
    val inbox = new QueueInbox(this, inboxCapacity)
    fn.create(inbox)
  }

  def handle(actor: Actor, message: Message, e: Throwable): Unit = {

  }

}
