package org.wisp.bus

import org.wisp.bus.EventBus
import org.wisp.ActorMessage

object UndeliveredMessage{

  def unapply(m: UndeliveredMessage): Some[ActorMessage] = Some(m.message)

}

class UndeliveredMessage(val message: ActorMessage) extends Event{

  override def toString = s"UndeliveredMessage($message)"
}
