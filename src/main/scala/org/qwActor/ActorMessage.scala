package org.qwActor

import java.util.function.Consumer

object ActorMessage{
  def apply(sender:ActorRef, value:Any) = new ActorMessage(sender, value)

  def unapply(m: ActorMessage): Some[(ActorRef, Any)] = Some((m.sender, m.value))

  val nil: Consumer[Any] = m => {
    if (logger.isWarnEnabled) logger.warn("ignored message: " + m)
  }
}

class ActorMessage(val sender:ActorRef, val value:Any){
  override def toString: String = "ActorMessage("+sender+", "+value+")"
}
