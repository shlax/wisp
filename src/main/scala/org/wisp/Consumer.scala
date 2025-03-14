package org.wisp

object Consumer {

  def apply[T](ref:ActorLink):Consumer[T] = (t: T) => { ref << t }
  
}

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
