package org.wisp.timer

import org.wisp.ActorRef
import org.wisp.bus.Event

import scala.concurrent.duration.Duration

object FailedTimer{

  def unapply(m: FailedTimer): Some[(Timer, ActorRef, Throwable)] = Some((m.timer, m.actorRef, m.exception))

}

abstract class FailedTimer(val timer:Timer, val actorRef: ActorRef, val exception: Throwable) extends Event{

  override def stackTrace: Option[Throwable] = Some(exception)

}
