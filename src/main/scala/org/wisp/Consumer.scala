package org.wisp

object Consumer {

  def apply[T](ref:ActorLink):Consumer[T] = {
    (t: T) => {
      ref << t
    }
  }
  
}

/** [[java.util.function.Consumer]] with added variance */
@FunctionalInterface
trait Consumer[-T] {

  /** [[java.util.function.Consumer#accept(java.lang.Object)]] */
  def accept(t:T):Unit

  def map[R](fn: R => T): Consumer[R] = {
    val self = this
    (e: R) => {
      self.accept(fn.apply(e))
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
      if (fn.apply(e)) self.accept(e)
    }
  }

  def collect[R](fn: PartialFunction[R, T]): Consumer[R] = {
    val self = this
    (e: R) => {
      if (fn.isDefinedAt(e)) self.accept(fn.apply(e))
    }
  }

  /** [[java.util.function.Consumer#andThen(java.util.function.Consumer)]] with added variance */
  def andThen[S <: T](after: Consumer[S]): Consumer[S] = {
    val self = this
    (t: S) => {
      self.accept(t)
      after.accept(t)
    }
  }

}
