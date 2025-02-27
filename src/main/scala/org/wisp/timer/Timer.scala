package org.wisp.timer

import org.wisp.ActorLink

import java.util.concurrent.{Callable, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import scala.concurrent.duration.Duration

class Timer extends AutoCloseable{

  protected val service: ScheduledExecutorService = createService()
  protected def createService(): ScheduledExecutorService = {
    Executors.newSingleThreadScheduledExecutor()
  }

  def schedule[V](link:ActorLink, delay:Duration, value: => V): ScheduledFuture[V] = {
    service.schedule( () => {
      val v = value
      link << v
      v
    }, delay.toNanos, TimeUnit.NANOSECONDS )
  }

  def scheduleAtFixedRate(link:ActorLink, initialDelay:Duration, period:Duration, callable: => Any): ScheduledFuture[?] = {
    service.scheduleAtFixedRate( () => {
      val v = callable
      link << v
    }, initialDelay.toNanos, period.toNanos, TimeUnit.NANOSECONDS )
  }

  def scheduleWithFixedDelay(link: ActorLink, initialDelay: Duration, delay: Duration, callable: => Any): ScheduledFuture[?] = {
    service.scheduleWithFixedDelay(() => {
      val v = callable
      link << v
    }, initialDelay.toNanos, delay.toNanos, TimeUnit.NANOSECONDS)
  }

  override def close(): Unit = {
    service.close()
  }

}
