package org.wisp

/** Represents execution runtime for actor. */
trait ActorScheduler[T] {

  /** Schedule `message` for execution. */
  def schedule(message: Message[T]):Unit

}
