package org.wisp.stream

import org.wisp.Consumer
import org.wisp.utils.lock.withLock

import java.util.concurrent.locks.ReentrantLock

object Sink {

  /**
   * Creates [[Sink]] from function
   */
  def apply[T](fn: T => Unit): Sink[T] = {
    (t: T) => {
      fn.apply(t)
    }
  }

}

/**
 * Extends [[Consumer]] with `complete` method
 */
@FunctionalInterface
trait Sink[-T] extends Consumer[T]{

  /**
   * Indicates end of stream
   */
  def complete(): Unit = {}

  override def map[R](fn: R => T): Sink[R] = {
    val self = this
    new Sink[R] {
      override def apply(e: R): Unit = {
        self.apply(fn.apply(e))
      }
      override def complete(): Unit = {
        self.complete()
      }
    }
  }

  override def flatMap[R](fn: (R, this.type) => Unit): Sink[R] = {
    val self:this.type = this
    new Sink[R] {
      override def apply(e: R): Unit = {
        fn.apply(e, self)
      }
      override def complete(): Unit = {
        self.complete()
      }
    }
  }

  override def filter[R <: T](fn: R => Boolean): Sink[R] = {
    val self = this
    new Sink[R] {
      override def apply(e: R): Unit = {
        if(fn.apply(e)) self.apply(e)
      }
      override def complete(): Unit = {
        self.complete()
      }
    }
  }

  override def collect[R](fn: PartialFunction[R, T]): Sink[R] = {
    val self = this
    new Sink[R] {
      override def apply(e: R): Unit = {
        if (fn.isDefinedAt(e)) self.apply(fn.apply(e))
      }

      override def complete(): Unit = {
        self.complete()
      }
    }
  }

  /**
   * Returns a composed `Sink` that performs, in sequence, this operation followed by the `after` operation.
   */
  def nextTo[S <: T](after: Sink[S]): Sink[S] = {
    val self = this
    new Sink[S]{

      override def apply(t: S): Unit = {
        self.apply(t)
        after.apply(t)
      }

      override def complete(): Unit = {
        self.complete()
        after.complete()
      }

    }
  }

  /**
   * Consume [[org.wisp.stream.Source]] and call `complete`
   */
  override def consume(s:Source[T]):Unit = {
    super.consume(s)
    complete()
  }

  /**
   * @return synchronized view over this [[Sink]]
   */
  override def withSynchronization(): Sink[T] = {
    val self = this
    new Sink[T]{
      private val lock = new ReentrantLock()

      override def apply(t: T): Unit = lock.withLock{
        self.apply(t)
      }
      override def complete(): Unit = lock.withLock{
        self.complete()
      }
    }
  }

}
