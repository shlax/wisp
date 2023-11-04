package org.miniActor

trait ActorRuntime {

  def create(fn: ActorContext => Actor): ActorRef

}
