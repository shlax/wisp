package org.wisp.timer

import org.wisp.ActorRef

import scala.concurrent.duration.Duration

object FailedTimerSchedule {

  def unapply(m: FailedTimerSchedule): Some[(Timer, ActorRef, Duration, Throwable)] = Some((m.timer, m.actorRef, m.delay, m.exception))

}

class FailedTimerSchedule(tim:Timer, ref: ActorRef, val delay:Duration, exc: Throwable) extends FailedTimer(tim, ref, exc){

  override def toString = s"FailedTimerSchedule($timer, $actorRef, $delay, $exception)"
}
