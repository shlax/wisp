package org.wisp.stream

import org.wisp.Consumer

@FunctionalInterface
trait Sink[-T] extends Consumer[T]{

  def flush(): Unit = {}

  def thenTo[S <: T](after: Sink[S]): Sink[S] = {
    val self = this
    new Sink[S]{

      override def accept(t: S): Unit = {
        self.accept(t)
        after.accept(t)
      }

      override def flush(): Unit = {
        self.flush()
        after.flush()
      }

    }
  }

}
