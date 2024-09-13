package org.wisp.timer

import org.wisp.ActorRef

import scala.concurrent.duration.Duration

object FailedTimerAtFixedRate{

  def unapply(m: FailedTimerAtFixedRate): Some[(Timer, ActorRef, Duration, Duration, Throwable)] ={
    Some((m.timer, m.actorRef, m.initialDelay, m.period, m.exception))
  }

}

class FailedTimerAtFixedRate(tim:Timer, ref: ActorRef, val initialDelay:Duration, val period:Duration, exc: Throwable) extends FailedTimer(tim, ref, exc){
    
  override def toString = s"FailedTimerAtFixedRate($timer, $actorRef $initialDelay, $period, $exception)"
}
