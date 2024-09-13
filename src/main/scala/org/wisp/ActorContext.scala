package org.wisp

import org.wisp.bus.EventBus

abstract class ActorContext(bus:ActorRuntime) extends ActorRef(bus) with ActorRuntime{
  override def execute(command: Runnable): Unit = bus.execute(command)
  override def publish(event:Any):Unit = bus.publish(event)

  def messageQueueSize():Int

}
