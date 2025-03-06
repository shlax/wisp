package org.wisp

trait Actor extends ActorLink{

  /** function with the actor logic */
  def accept(from:ActorLink): PartialFunction[Any, Unit]

  protected def inbox: Inbox

  /** redirect message from [[ActorLink]] to [[inbox]] */
  override def accept(m: Message): Unit = {
    inbox.add(m)
  }

}
