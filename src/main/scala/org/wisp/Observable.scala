package org.wisp

import java.util

trait Observable[T] extends Consumer[T]{

  trait Subscription {

    def subscriber: T => Unit

    def cancel():Boolean
  }

  def subscribe(subscriber: T => Unit): Subscription

}

object Observable {

  def apply[T](): AbstractObservable[T] = new AbstractObservable[T]{
    override protected val subscriptions: util.List[Unsubscribe] = util.LinkedList[Unsubscribe]()
  }

}
