package org.wisp.bus

import org.wisp.{Actor, ActorMessage}

object FailedMessage{

  def unapply(m: FailedMessage): Some[(Actor, ActorMessage, Throwable)] = Some((m.actor, m.message, m.exception))
}

class FailedMessage(val actor: Actor, val message: ActorMessage, val exception: Throwable)extends Event{

  override def toString = s"FailedMessage($actor, $message, $exception)"
}
