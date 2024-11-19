package org.wisp

trait Inbox {

  def add(actor: Actor, message: Message):Unit

}
