package org.wisp

trait ActorRuntime {

  def create(fn: ActorContext => Actor): ActorRef

}
