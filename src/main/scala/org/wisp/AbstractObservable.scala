package org.wisp

import java.util
import java.util.concurrent.locks.ReentrantLock
import org.wisp.lock.*

abstract class AbstractObservable[T] extends Observable[T]{

  protected def subscriptions: util.List[AbstractSubscription]

  class AbstractSubscription(override val subscriber: T => Unit) extends Subscription {
    def cancel(): Boolean = { subscriptions.remove(this) }
  }

  override def subscribe(subscriber: T => Unit): Subscription = {
    val s = new AbstractSubscription(subscriber)
    subscriptions.add(s)
    s
  }

  override def accept(t: T): Unit = {
    subscriptions.forEach { i =>
      i.subscriber.apply(t)
    }
  }

  def withSynchronization(): Observable[T] = {
    val self = this
    new Observable[T]{
      private val lock = ReentrantLock()

      override def subscribe(fn: T => Unit): Subscription = lock.withLock {
        val s = self.subscribe(fn)
        new Subscription{
          override def subscriber: T => Unit = s.subscriber
          override def cancel(): Boolean = lock.withLock {
            s.cancel()
          }
        }
      }

      override def accept(t: T): Unit = lock.withLock {
        self.accept(t)
      }

    }
  }

}
