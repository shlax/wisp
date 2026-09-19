package org.wisp.observable

import org.wisp.Consumer
import org.wisp.utils.lock.*
import java.util.concurrent.locks.ReentrantLock

trait Observable[T] extends Consumer[T]{

  trait Subscription {

    def subscriber: T => Unit

    def cancel():Boolean
  }

  def to(subscriber: T => Unit): Subscription

  def mapTo[U](f: T => U): Observable[U] = {
    val n = Observable[U]()
    to{ (v: T) =>
      val y = f.apply(v)
      n.apply(y)
    }
    n
  }

  def flatMapTo[U](f: (T, Observable[U]) => Unit): Observable[U] = {
    val n = Observable[U]()
    to { (v: T) =>
      f.apply(v, n)
    }
    n
  }

  def filterTo(f: T => Boolean): Observable[T] = {
    val n = Observable[T]()
    to{ (v: T) =>
      if(f.apply(v)){
        n.apply(v)
      }
    }
    n
  }

  def collectTo[U](f:PartialFunction[T, U]): Observable[U] = {
    val n = Observable[U]()
    to{ (v: T) =>
      if(f.isDefinedAt(v)){
        n.apply(f.apply(v))
      }
    }
    n
  }

  def as[U](f: this.type => U): U = {
    f.apply(this)
  }

  /**
   * @return synchronized view over this [[Observable]]
   */
  override def withSynchronization(): Observable[T] = {
    val self = this
    new Observable[T] {
      private val lock = ReentrantLock()

      override def to(fn: T => Unit): Subscription = lock.withLock {
        val s = self.to(fn)
        new Subscription {
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

object Observable {

  def apply[T](): Observable[T] ={
    new DefaultObservable[T]
  }

}
