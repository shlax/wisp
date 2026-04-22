package org.wisp.timer

import org.wisp.ActorLink

import java.util.concurrent.{Callable, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import scala.concurrent.duration.Duration

class Timer[T] extends AutoCloseable{

  protected val service: ScheduledExecutorService = createService()
  protected def createService(): ScheduledExecutorService = {
    Executors.newSingleThreadScheduledExecutor()
  }

  def schedule(link:ActorLink[T], delay:Duration, value: => T): ScheduledFuture[T] = {
    service.schedule( () => {
      val v = value
      link << v
      v
    }, delay.toNanos, TimeUnit.NANOSECONDS )
  }

  def scheduleAtFixedRate(link:ActorLink[T], initialDelay:Duration, period:Duration, callable: => T): ScheduledFuture[?] = {
    service.scheduleAtFixedRate( () => {
      val v = callable
      link << v
    }, initialDelay.toNanos, period.toNanos, TimeUnit.NANOSECONDS )
  }

  def scheduleWithFixedDelay(link: ActorLink[T], initialDelay: Duration, delay: Duration, callable: => T): ScheduledFuture[?] = {
    service.scheduleWithFixedDelay(() => {
      val v = callable
      link << v
    }, initialDelay.toNanos, delay.toNanos, TimeUnit.NANOSECONDS)
  }

  override def close(): Unit = {
    service.close()
  }

}
