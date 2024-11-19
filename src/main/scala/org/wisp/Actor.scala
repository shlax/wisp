package org.wisp

abstract class Actor(val inbox: Inbox) extends ActorRef{

  def accept(from:ActorRef): PartialFunction[Any, Unit]

  override def accept(m: Message): Unit = {
    inbox.add(this, m)
  }

}
