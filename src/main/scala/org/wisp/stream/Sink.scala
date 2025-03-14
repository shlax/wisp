package org.wisp.stream

import org.wisp.{ActorLink, Consumer}

object Sink {

  def apply[T](ref:ActorLink):Sink[T] = (t: T) => { ref << t }

}

@FunctionalInterface
trait Sink[-T] extends Consumer[T]{

  /** Force element emission
   * @note for grouping operation */
  def flush(): Unit = {}

  def forEach(src: Source[T]): Unit = {
    src.forEach(this)
    flush()
  }

  /** Returns a composed `Sink` that performs, in sequence, this operation followed by the `after` operation. */
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
