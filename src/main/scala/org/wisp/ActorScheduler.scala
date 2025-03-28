package org.wisp

/** Represents execution runtime for actor. */
trait ActorScheduler {

  /** Schedule `message` for execution. */
  def schedule(message: Message):Unit

}
