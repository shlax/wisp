package org.wisp

/**
 * Actor base trait
 */
trait Actor[T] extends ActorLink{

  /** Function with the actor logic */
  def apply(from:ActorLink): PartialFunction[T, Unit]

  /** Reference to execution runtime. */
  protected def scheduler: ActorScheduler

  /** redirect message to [[scheduler]] */
  override def apply(m: Message): Unit = {
    scheduler.schedule(m)
  }

}
