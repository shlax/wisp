package org.wisp

@FunctionalInterface
trait ActorCreator {

  def create(i:Inbox) : Actor

}
