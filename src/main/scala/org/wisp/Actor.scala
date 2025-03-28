package org.wisp

trait Actor extends ActorLink{

  /** Function with the actor logic */
  def accept(from:ActorLink): PartialFunction[Any, Unit]

  protected def scheduler: ActorScheduler

  /** redirect message to [[scheduler]] */
  override def accept(m: Message): Unit = {
    scheduler.schedule(m)
  }

}
