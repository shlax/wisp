package org.qwActor

trait ActorRuntime {

  def create(fn: ActorContext => Actor): ActorRef

}
