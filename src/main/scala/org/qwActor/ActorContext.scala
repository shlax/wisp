package org.qwActor

trait ActorContext extends ActorRef, ActorRuntime{

  def messageQueueSize():Int

}
