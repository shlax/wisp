package org.wisp.stream

import org.wisp.ActorLink

import java.{lang, util}
import scala.concurrent.{Future, Promise}
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

  extension (i: ActorLink) {
    def asSink[E]: Sink[E] = (t: E) => {
      i << t
    }
  }

  extension [E](p: Promise[E]) {
    def asSink[T](start: E)(fn: (E, T) => E): (Sink[T], Future[E]) = {
      val s = new Sink[T] {
        private var value: E = start

        override def accept(t: T): Unit = {
          try {
            value = fn(value, t)
          } catch {
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

  extension [E](s: Iterable[Sink[E]]) {
    def asSink: Sink[E] = new Sink[E] {
      override def accept(t: E): Unit = {
        for (i <- s) i.accept(t)
      }

      override def flush(): Unit = {
        for (i <- s) i.flush()
      }
    }
  }

}
