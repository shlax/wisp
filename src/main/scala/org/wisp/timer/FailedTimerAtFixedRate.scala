package org.wisp.timer

import org.wisp.ActorRef

import scala.concurrent.duration.Duration

object FailedTimerAtFixedRate{

  def unapply(m: FailedTimerAtFixedRate): Some[(Timer, ActorRef, Duration, Duration, Throwable)] ={
    Some((m.timer, m.actorRef, m.initialDelay, m.period, m.exception))
  }

}

class FailedTimerAtFixedRate(timer:Timer, actorRef: ActorRef, val initialDelay:Duration, val period:Duration, exception: Throwable) extends FailedTimer(timer, actorRef, exception)
