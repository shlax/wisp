package org.wisp.observable

import org.wisp.utils.lock.*

import java.util
import java.util.concurrent.locks.ReentrantLock

abstract class AbstractObservable[T] extends Observable[T]{

  protected def subscriptions: util.Collection[CollectionSubscription]

  class CollectionSubscription(override val subscriber: T => Unit) extends Subscription {
    def cancel(): Boolean = {
      subscriptions.remove(this)
    }
  }

  override def subscribe(subscriber: T => Unit): Subscription = {
    val s = new CollectionSubscription(subscriber)
    subscriptions.add(s)
    s
  }

  override def apply(t: T): Unit = {
    subscriptions.forEach { i =>
      i.subscriber.apply(t)
    }
  }

  /**
   * @return synchronized view over this [[Observable]]
   */
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

      override def apply(t: T): Unit = lock.withLock {
        self.apply(t)
      }

    }
  }

}
