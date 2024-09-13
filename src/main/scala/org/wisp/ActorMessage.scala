package org.wisp

import org.wisp.bus.EventBus

import java.util.function.Consumer

object ActorMessage{
  def apply(sender:ActorRef, value:Any) = new ActorMessage(sender, value)

  def unapply(m: ActorMessage): Some[(ActorRef, Any)] = Some((m.sender, m.value))
  
}

class ActorMessage(val sender:ActorRef, val value:Any){
  override def toString: String = "ActorMessage("+sender+", "+value+")"
}
