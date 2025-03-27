package org.wisp

/** Represents execution runtime for actor. */
trait Inbox {

  /** Schedule `message` for execution. */
  def add(message: Message):Unit

}
