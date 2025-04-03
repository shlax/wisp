package org.wisp.stream

import org.wisp.Consumer

object Sink {

  def apply[T](fn: T => Unit): Sink[T] = {
    (t: T) => {
      fn.apply(t)
    }
  }

}

@FunctionalInterface
trait Sink[-T] extends Consumer[T]{

  /** Indicates end of stream
   * @note for grouping operation */
  def complete(): Unit = {}

  override def map[R](fn: R => T): Sink[R] = {
    val self = this
    new Sink[R] {
      override def accept(e: R): Unit = {
        self.accept(fn.apply(e))
      }
      override def complete(): Unit = {
        self.complete()
      }
    }
  }

  override def flatMap[R](fn: (R, this.type) => Unit): Sink[R] = {
    val self:this.type = this
    new Sink[R] {
      override def accept(e: R): Unit = {
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
      override def accept(e: R): Unit = {
        if(fn.apply(e)) self.accept(e)
      }
      override def complete(): Unit = {
        self.complete()
      }
    }
  }

  override def collect[R](fn: PartialFunction[R, T]): Sink[R] = {
    val self = this
    new Sink[R] {
      override def accept(e: R): Unit = {
        if (fn.isDefinedAt(e)) self.accept(fn.apply(e))
      }

      override def complete(): Unit = {
        self.complete()
      }
    }
  }

  /** Returns a composed `Sink` that performs, in sequence, this operation followed by the `after` operation. */
  def thenTo[S <: T](after: Sink[S]): Sink[S] = {
    val self = this
    new Sink[S]{

      override def accept(t: S): Unit = {
        self.accept(t)
        after.accept(t)
      }

      override def complete(): Unit = {
        self.complete()
        after.complete()
      }

    }
  }

  override def consume(s:Source[T]):Unit = {
    super.consume(s)
    complete()
  }

}
