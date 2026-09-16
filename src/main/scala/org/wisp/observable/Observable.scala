package org.wisp.observable

import org.wisp.Consumer

import java.util

trait Observable[T] extends Consumer[T]{

  trait Subscription {

    def subscriber: T => Unit

    def cancel():Boolean
  }

  def subscribe(subscriber: T => Unit): Subscription

  def map[U](f: T => U): Observable[U] = {
    val n = Observable[U]()
    subscribe{ (v: T) =>
      val y = f.apply(v)
      n.apply(y)
    }
    n
  }

  def flatMap[U](f: (T, Observable[U]) => Unit): Observable[U] = {
    val n = Observable[U]()
    subscribe { (v: T) =>
      f.apply(v, n)
    }
    n
  }

  def filter(f: T => Boolean): Observable[T] = {
    val n = Observable[T]()
    subscribe{ (v: T) =>
      if(f.apply(v)){
        n.apply(v)
      }
    }
    n
  }

  def collect[U](f:PartialFunction[T, U]): Observable[U] = {
    val n = Observable[U]()
    subscribe{ (v: T) =>
      if(f.isDefinedAt(v)){
        n.apply(f.apply(v))
      }
    }
    n
  }

  def as[U](f: this.type => U): U = {
    f.apply(this)
  }

}

object Observable {

  def apply[T](): AbstractObservable[T] = new AbstractObservable[T]{
    override protected val subscriptions: util.Collection[CollectionSubscription] = util.LinkedList[CollectionSubscription]()
  }

}
