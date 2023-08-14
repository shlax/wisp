package org.qwActor

import java.util.function.Consumer

object ActorMessage{
  def apply(sender:ActorRef, value:Any, handler:Option[Consumer[Any]] = None) = new ActorMessage(sender, value)

  def unapply(m: ActorMessage): Some[(ActorRef, Any, Option[Consumer[Any]])] = Some((m.sender, m.value, m.handler))

  val nil: Consumer[Any] = m => {
    if (logger.isWarnEnabled) logger.warn("ignored message: " + m)
  }
}

class ActorMessage(val sender:ActorRef, val value:Any, val handler:Option[Consumer[Any]] = None){
  override def toString: String = "ActorMessage("+sender+", "+value+")"
}
