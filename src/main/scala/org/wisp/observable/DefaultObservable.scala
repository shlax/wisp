package org.wisp.observable

import java.util

class DefaultObservable[T] extends Observable[T]{

  protected val subscriptions: util.Collection[CollectionSubscription] = createSubscriptions()

  protected def createSubscriptions(): util.Collection[CollectionSubscription] = {
    util.LinkedList[CollectionSubscription]()
  }

  class CollectionSubscription(override val subscriber: T => Unit) extends Subscription {
    def cancel(): Boolean = {
      subscriptions.remove(this)
    }
  }

  override def to(subscriber: T => Unit): Subscription = {
    val s = new CollectionSubscription(subscriber)
    subscriptions.add(s)
    s
  }

  override def apply(t: T): Unit = {
    subscriptions.forEach { i =>
      i.subscriber.apply(t)
    }
  }



}
