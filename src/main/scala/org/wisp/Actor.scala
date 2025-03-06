package org.wisp

trait Actor extends ActorLink{

  def accept(from:ActorLink): PartialFunction[Any, Unit]

  protected def inbox: Inbox
  
  override def accept(m: Message): Unit = {
    inbox.add(m)
  }

}
