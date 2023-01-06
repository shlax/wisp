package org.qwActor

import java.util.concurrent.{BlockingQueue, ConcurrentLinkedQueue, LinkedBlockingQueue}
import scala.annotation.targetName

abstract class Actor(context: ActorContext) extends ActorRef {

  def createQueue() : MessageQueue[ActorMessage] = MessageQueue()

  def process(sender:ActorRef): PartialFunction[Any, Unit]

  override def accept(msg: ActorMessage): Unit = context.accept(msg)

  @targetName("send")
  override def << (value: Any): Unit = accept(this, value)

}
