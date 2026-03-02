package org.wisp.utils.observable

import org.wisp.utils.lock.*

import java.util
import java.util.concurrent.locks.ReentrantLock

abstract class AbstractObservable[T] extends Observable[T]{

  protected def subscriptions: util.List[ListSubscription]

  class ListSubscription(override val subscriber: T => Unit) extends Subscription {
    def cancel(): Boolean = { subscriptions.remove(this) }
  }

  override def subscribe(subscriber: T => Unit): Subscription = {
    val s = new ListSubscription(subscriber)
    subscriptions.add(s)
    s
  }

  override def accept(t: T): Unit = {
    subscriptions.forEach { i =>
      i.subscriber.apply(t)
    }
  }

  /** @return synchronized view over this [[Observable]] */
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
