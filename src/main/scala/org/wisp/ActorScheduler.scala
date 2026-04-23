package org.wisp

/**
 * Represents execution runtime for actor.
 */
trait ActorScheduler[-T, +R] {

  /**
   * Schedule `message` for execution.
   */
  def schedule(message: Message[T, R]):Unit

}
