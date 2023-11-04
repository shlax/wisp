package org.miniActor

import scala.annotation.targetName

object Actor{

  def apply(ref: ActorRef, context: ActorContext): Actor = new Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case v => ref.accept(new ActorMessage(sender, v))
    }
  }
  
}

abstract class Actor(context: ActorContext) extends ActorRef {

  def createQueue() : MessageQueue[ActorMessage] = MessageQueue()

  def process(sender:ActorRef): PartialFunction[Any, Unit]

  override def accept(msg: ActorMessage): Unit = context.accept(msg)

  @targetName("send")
  override def << (value: Any): Unit = accept(this, value)

}
