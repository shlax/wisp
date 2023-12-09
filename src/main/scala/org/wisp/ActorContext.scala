package org.wisp

import scala.annotation.targetName

trait ActorContext extends ActorRef, ActorRuntime{

  def messageQueueSize():Int

}
