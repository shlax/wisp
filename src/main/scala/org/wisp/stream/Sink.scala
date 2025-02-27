package org.wisp.stream

import org.wisp.Consumer

@FunctionalInterface
trait Sink[-T] extends Consumer[T]{

  def flush(): Unit = {}

  def andThen[S <: T](after: Consumer[S]): Consumer[S] = {
    val self = this
    (t: S) => {
      self.accept(t)
      after.accept(t)
    }
  }

}
