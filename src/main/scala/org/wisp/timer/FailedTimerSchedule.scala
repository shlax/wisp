package org.wisp.timer

import org.wisp.ActorRef

import scala.concurrent.duration.Duration

object FailedTimerSchedule {

  def unapply(m: FailedTimerSchedule): Some[(Timer, ActorRef, Duration, Throwable)] = Some((m.timer, m.actorRef, m.delay, m.exception))

}

class FailedTimerSchedule(timer:Timer, actorRef: ActorRef, val delay:Duration, exception: Throwable) extends FailedTimer(timer, actorRef, exception)
