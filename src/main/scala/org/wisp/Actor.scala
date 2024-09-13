package org.wisp

import scala.annotation.targetName

object Actor{

  def apply(ref: ActorRef, context: ActorContext): Actor = new Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case v => ref.accept(new ActorMessage(sender, v))
    }
  }
  
}

abstract class Actor(context: ActorContext) extends ActorRef(context) {

  def createQueue() : MessageQueue[ActorMessage] = MessageQueue()

  def process(sender:ActorRef): PartialFunction[Any, Unit]

  override def accept(msg: ActorMessage): Unit = context.accept(msg)

  @targetName("send")
  override def << (value: Any): Unit = accept(this, value)

  /** set sender to this */
  protected def wrap(r: ActorRef): ActorRef = new ActorRef(r.eventBus) {
    @targetName("send")
    override def <<(value: Any): Unit = r.accept(Actor.this, value)

    override def accept(t: ActorMessage): Unit = r.accept(t)
  }

}
