package org.wisp.stream

import org.wisp.Link

import java.{lang, util}
import scala.concurrent.Promise
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

object extensions {

  extension [E](i: util.Iterator[E]) {
    def asSource: Source[E] = { () =>
      if (i.hasNext) Some(i.next()) else None
    }
  }

  extension [E](i: util.Enumeration[E]) {
    def asSource: Source[E] = { () =>
      if (i.hasMoreElements) Some(i.nextElement()) else None
    }
  }

  extension [E](it: lang.Iterable[E]) {
    def asSource: Source[E] = new Source[E] {
      private val i = it.iterator()

      override def next(): Option[E] = if (i.hasNext) Some(i.next()) else None
    }
  }

  extension [K, V](it: util.Map[K, V]) {
    def asSource: Source[util.Map.Entry[K, V]] = new Source[util.Map.Entry[K, V]] {
      private val i = it.entrySet().iterator()

      override def next(): Option[util.Map.Entry[K, V]] = if (i.hasNext) Some(i.next()) else None
    }
  }

  extension [E](it: IterableOnce[E]) {
    def asSource: Source[E] = new Source[E] {
      private val i = it.iterator

      override def next(): Option[E] = if (i.hasNext) Some(i.next()) else None
    }
  }

  extension [E](it: Array[E]) {
    def asSource: Source[E] = new Source[E] {
      private val i = it.iterator

      override def next(): Option[E] = if (i.hasNext) Some(i.next()) else None
    }
  }

  extension [E, T](i: Link[E, T]) {
    def asSink: Sink[E] = (t: E) => {
      i << t
    }
  }

  extension [E](promise: Promise[E]) {

    /**
     * Converts [[scala.concurrent.Promise]] to [[org.wisp.stream.Sink]] where `promise` wil be completed with result of `fold` function.
     */
    def asSink[T](start: E)(fold: (E, T) => E): Sink[T] = {
      new Sink[T] {
        private var value: E = start

        override def apply(t: T): Unit = {
          try {
            value = fold(value, t)
          } catch {
            case NonFatal(e) =>
              promise.failure(e)
              throw e
          }
        }

        override def complete(): Unit = {
          promise.success(value)
        }

      }
    }

  }

  extension [E](iterable: Iterable[Sink[E]]) {

    /**
     * @return [[Sink]] that will call all sinks in `iterable`
     */
    def asSink: Sink[E] = new Sink[E] {
      override def apply(t: E): Unit = {
        for (i <- iterable) i.apply(t)
      }

      override def complete(): Unit = {
        for (i <- iterable) i.complete()
      }
    }

  }

}
