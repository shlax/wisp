package org.wisp

/** [[java.util.function.Consumer]] with added variance */
@FunctionalInterface
trait Consumer[-T] {

  /** [[java.util.function.Consumer#accept(java.lang.Object)]] */
  def accept(t:T):Unit

  /** [[java.util.function.Consumer#andThen(java.util.function.Consumer)]] with added variance */
  def andThen[S <: T](after: Consumer[S]): Consumer[S] = {
    val self = this
    (t: S) => {
      self.accept(t)
      after.accept(t)
    }
  }

}
