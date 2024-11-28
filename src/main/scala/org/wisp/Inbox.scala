package org.wisp

trait Inbox {

  def system: ActorSystem

  def add(message: Message):Unit

}
