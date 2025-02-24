package org.wisp

@FunctionalInterface
trait ActorFactory[T <: Actor] {

  def create(i:Inbox) : T

}
