package org.wisp

@FunctionalInterface
trait Inbox {

  def add(actor: Actor, message: Message):Unit

}
