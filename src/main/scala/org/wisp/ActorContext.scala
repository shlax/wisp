package org.wisp

trait ActorContext extends ActorRef, ActorRuntime{

  def messageQueueSize():Int

}
