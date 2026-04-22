package org.wisp

/**
 * Actor base trait
 */
trait Actor[T] extends ActorLink[T]{

  /** Function with the actor logic */
  def apply(from:ActorLink[Any]): PartialFunction[T, Unit]

  /** Reference to execution runtime. */
  protected def scheduler: ActorScheduler[T]

  /** redirect message to [[scheduler]] */
  override def apply(m: Message[T]): Unit = {
    scheduler.schedule(m)
  }

}
