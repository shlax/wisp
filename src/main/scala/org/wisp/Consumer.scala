package org.wisp

import org.wisp.stream.Source

object Consumer {

  /** Creates [[Consumer]] from function */
  def apply[T](fn: T => Unit): Consumer[T] = {
    (t: T) => {
      fn.apply(t)
    }
  }

  /** Creates [[Consumer]] that sends messages to [[ActorLink]] */
  def apply[T](ref:ActorLink[T]):Consumer[T] = {
    (t: T) => {
      ref << t
    }
  }
  
}

/** [[java.util.function.Consumer]] with added variance */
@FunctionalInterface
trait Consumer[-T] extends ( T => Unit ) {

  /** [[java.util.function.Consumer#accept(java.lang.Object)]] */
  override def apply(t:T):Unit

  def map[R](fn: R => T): Consumer[R] = {
    val self = this
    (e: R) => {
      self.apply(fn.apply(e))
    }
  }

  def flatMap[R](fn: (R, this.type) => Unit): Consumer[R] = {
    val self: this.type = this
    (e: R) => {
      fn.apply(e, self)
    }
  }

  def filter[R <: T](fn: R => Boolean): Consumer[R] = {
    val self = this
    (e: R) => {
      if (fn.apply(e)) self.apply(e)
    }
  }

  def collect[R](fn: PartialFunction[R, T]): Consumer[R] = {
    val self = this
    (e: R) => {
      if (fn.isDefinedAt(e)) self.apply(fn.apply(e))
    }
  }

  /** [[java.util.function.Consumer#andThen(java.util.function.Consumer)]] with added variance */
  def andThen[S <: T](after: S => Unit): Consumer[S] = {
    val self = this
    (t: S) => {
      self.apply(t)
      after.apply(t)
    }
  }

  def consume(s: Source[T]): Unit = {
    s.forEach(apply)
  }
  
}
