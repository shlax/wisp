package org.wisp

@FunctionalInterface
trait Consumer[-T] {

  def accept(t:T):Unit

  def andThen[S <: T](after: Consumer[S]): Consumer[S] = {
    val self = this
    (t: S) => {
      self.accept(t)
      after.accept(t)
    }
  }

}
