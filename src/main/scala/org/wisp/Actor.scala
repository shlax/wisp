package org.wisp

abstract class Actor(val inbox: Inbox) extends ActorLink{
  
  def accept(from:ActorLink): PartialFunction[Any, Unit]

  override def accept(m: Message): Unit = {
    inbox.add(m)
  }

}
