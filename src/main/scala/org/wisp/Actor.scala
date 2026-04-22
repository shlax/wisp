package org.wisp

/**
 * Actor base trait
 */
trait Actor[T, R] extends Link[T, R]{

  /**
   * Function with the actor logic
   */
  def apply(from:Link[R, T]) : T => Unit

  /**
   * Reference to execution runtime.
   */
  protected def scheduler: ActorScheduler[T, R]

  /**
   * Redirect message to [[scheduler]]
   */
  override def apply(m: LinkCallback[T, R]): Unit = {
    scheduler.schedule(m)
  }

}
