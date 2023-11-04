package org.miniActor

trait ActorContext extends ActorRef, ActorRuntime{

  def messageQueueSize():Int

}
