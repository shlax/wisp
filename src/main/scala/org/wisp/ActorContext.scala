package org.wisp

import org.wisp.bus.{Event, EventBus}

abstract class ActorContext(bus:ActorRuntime) extends ActorRef(bus) with ActorRuntime{
  override def execute(command: Runnable): Unit = bus.execute(command)
  override def publish(event:Event):Unit = bus.publish(event)

  def messageQueueSize():Int

}
