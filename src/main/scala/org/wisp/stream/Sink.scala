package org.wisp.stream

import org.wisp.{ActorLink, Consumer}

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

object Sink {

  extension (i: ActorLink) {
    def asSink[E]: Sink[E] = (t: E) => {
      i << t
    }
  }

  extension [E](p: Promise[E]) {
    def asSink[T](start:E)(fn: (E, T) => E): (Sink[T], Future[E]) = {
      val s = new Sink[T]{
        private var value: E = start
        override def accept(t: T): Unit = {
          try {
            value = fn(value, t)
          }catch {
            case NonFatal(e) =>
              p.failure(e)
          }
        }
        override def flush(): Unit = {
          p.success(value)
        }
      }
      (s, p.future)
    }
  }

  extension [E](s: Iterable[Sink[? >: E]]) {
    def asSink: Sink[E] = new Sink[E] {
      override def accept(t: E): Unit = {
        for (i <- s) i.accept(t)
      }

      override def flush(): Unit = {
        for (i <- s) i.flush()
      }
    }
  }

  def apply[T](fn: T => Unit): Sink[T] = {
    (t: T) => { fn.apply(t) }
  }

}

@FunctionalInterface
trait Sink[-T] extends Consumer[T]{

  /** Force element emission
   * @note for grouping operation */
  def flush(): Unit = {}

  override def map[R](fn: R => T): Sink[R] = {
    val self = this
    new Sink[R] {
      override def accept(e: R): Unit = {
        self.accept(fn.apply(e))
      }
      override def flush(): Unit = {
        self.flush()
      }
    }
  }

  override def flatMap[R](fn: (R, this.type) => Unit): Sink[R] = {
    val self:this.type = this
    new Sink[R] {
      override def accept(e: R): Unit = {
        fn.apply(e, self)
      }
      override def flush(): Unit = {
        self.flush()
      }
    }
  }

  override def filter[R <: T](fn: R => Boolean): Sink[R] = {
    val self = this
    new Sink[R] {
      override def accept(e: R): Unit = {
        if(fn.apply(e)) self.accept(e)
      }
      override def flush(): Unit = {
        self.flush()
      }
    }
  }

  override def collect[R](fn: PartialFunction[R, T]): Sink[R] = {
    val self = this
    new Sink[R] {
      override def accept(e: R): Unit = {
        if (fn.isDefinedAt(e)) self.accept(fn.apply(e))
      }

      override def flush(): Unit = {
        self.flush()
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

      override def flush(): Unit = {
        self.flush()
        after.flush()
      }

    }
  }

}
