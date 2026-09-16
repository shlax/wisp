package org.wisp.timer

import org.wisp.observable.{AbstractObservable, Observable}
import org.wisp.Link

import java.util.concurrent.{Callable, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import scala.concurrent.duration.Duration

class Timer[T](scheduledService:Option[ScheduledExecutorService] = None) extends AutoCloseable{

  protected val service: ScheduledExecutorService = createService()

  /**
   * Use provided [[scheduledService]] or creates a single-threaded executor
   */
  protected def createService(): ScheduledExecutorService = {
    scheduledService match {
      case Some(s) => s
      case None =>
        Executors.newSingleThreadScheduledExecutor()
    }
  }

  /**
   * Schedules a given value to be sent asynchronously to the specified `Link` after a specified delay.
   * The value is computed lazily, and once the delay elapses, it is sent via the `<<` operator
   * of the provided `Link`. The scheduled task returns the computed value.
   */
  def schedule(link:Link[T, ?], delay:Duration, value: => T): ScheduledFuture[T] = {
    service.schedule( () => {
      val v:T = value
      link << v
      v
    }, delay.toNanos, TimeUnit.NANOSECONDS )
  }

  /**
   * Schedules a task to execute after a specified delay with a given value. The value is lazily evaluated,
   * and the provided function is executed on an `AbstractObservable` for additional subscriptions or behavior.
   */
  def schedule(delay:Duration, value: => T)(fn: AbstractObservable[T] => Unit): ScheduledFuture[T] = {
    val c = Observable[T]()
    fn.apply(c)
    schedule(!c, delay, value)
  }

  def schedule(consumer: T => Unit, delay:Duration, value: => T): ScheduledFuture[T] = {
    service.schedule( () => {
      val v:T = value
      consumer.apply(v)
      v
    }, delay.toNanos, TimeUnit.NANOSECONDS )
  }

  /**
   * Schedules a task to execute at a fixed rate after an initial delay.
   * The task computes a value of type `T` using the provided `callable` function
   * and sends it asynchronously to the specified `Link` using the `<<` operator.
   */
  def scheduleAtFixedRate(link:Link[T, ?], initialDelay:Duration, period:Duration, callable: => T): ScheduledFuture[?] = {
    service.scheduleAtFixedRate( () => {
      val v:T = callable
      link << v
    }, initialDelay.toNanos, period.toNanos, TimeUnit.NANOSECONDS )
  }

  /**
   * Schedules a task to execute at a fixed rate after an initial delay. The value is lazily evaluated,
   * and the provided function is executed on an `AbstractObservable` for additional subscriptions or behavior.
   */
  def scheduleAtFixedRate(initialDelay:Duration, period:Duration, callable: => T)(fn: AbstractObservable[T] => Unit): ScheduledFuture[?] = {
    val c = Observable[T]()
    fn.apply(c)
    scheduleAtFixedRate(!c, initialDelay, period, callable)
  }

  def scheduleAtFixedRate(consumer: T => Unit, initialDelay:Duration, period:Duration, callable: => T): ScheduledFuture[?] = {
    service.scheduleAtFixedRate( () => {
      val v:T = callable
      consumer.apply(v)
    }, initialDelay.toNanos, period.toNanos, TimeUnit.NANOSECONDS )
  }

  /**
   * Schedules a task to execute repeatedly with a fixed delay between the completion of one
   * execution and the start of the next. The task computes a value of type `T` using the provided
   * `callable` function and sends it asynchronously to the given `Link` using the `<<` operator.
   */
  def scheduleWithFixedDelay(link: Link[T, ?], initialDelay: Duration, delay: Duration, callable: => T): ScheduledFuture[?] = {
    service.scheduleWithFixedDelay(() => {
      val v:T = callable
      link << v
    }, initialDelay.toNanos, delay.toNanos, TimeUnit.NANOSECONDS)
  }

  /**
   * Schedules a task to execute at a fixed delay an initial delay. The value is lazily evaluated,
   * and the provided function is executed on an `AbstractObservable` for additional subscriptions or behavior.
   */
  def scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, callable: => T)(fn: AbstractObservable[T] => Unit): ScheduledFuture[?] = {
    val c = Observable[T]()
    fn.apply(c)
    scheduleWithFixedDelay(!c, initialDelay, delay, callable)
  }

  def scheduleWithFixedDelay(consumer: T => Unit, initialDelay: Duration, delay: Duration, callable: => T): ScheduledFuture[?] = {
    service.scheduleWithFixedDelay(() => {
      val v:T = callable
      consumer.apply(v)
    }, initialDelay.toNanos, delay.toNanos, TimeUnit.NANOSECONDS)
  }

  /**
   * Closes [[service]] only in case it was created by this Timer
   */
  override def close(): Unit = {
    if(scheduledService.isEmpty) {
      service.close()
    }
  }

}
