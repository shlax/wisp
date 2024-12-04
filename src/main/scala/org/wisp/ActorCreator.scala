package org.wisp

@FunctionalInterface
trait ActorCreator[T <: Actor] {

  def create(i:Inbox) : T

}
