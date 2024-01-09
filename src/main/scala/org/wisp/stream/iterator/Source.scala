package org.wisp.stream.iterator

import java.lang
import java.util
import java.util.function.Consumer

object Source{

  extension[E](i: util.Iterator[E]){
    def asSource: Source[E] = {
      () => if (i.hasNext) Some(i.next()) else None
    }
  }

  extension[E] (it: lang.Iterable[E]) {
    def asSource: Source[E] = new Source[E] {
      private val i = it.iterator()
      override def next(): Option[E] = if (i.hasNext) Some(i.next()) else None
    }
  }

  extension[K, V] (it: util.Map[K, V]) {
    def asSource: Source[util.Map.Entry[K, V]] = new Source[util.Map.Entry[K, V]] {
      private val i = it.entrySet().iterator()
      override def next(): Option[util.Map.Entry[K, V]] = if (i.hasNext) Some(i.next()) else None
    }
  }

  extension[E](it: IterableOnce[E]){
    def asSource: Source[E] = new Source[E] {
      private val i = it.iterator
      override def next(): Option[E] = if (i.hasNext) Some(i.next()) else None
    }
  }

  extension[E](it: Array[E]){
    def asSource: Source[E] = new Source[E] {
      private val i = it.iterator
      override def next(): Option[E] = if (i.hasNext) Some(i.next()) else None
    }
  }

}

trait Source[T] {

  /** {{{if(hasNext) Some(next()) else None}}} */
  def next():Option[T]

  def forEach(c: Consumer[_ >: T]):Unit = {
    var v = next()
    while (v.isDefined){
      c.accept(v.get)
      v = next()
    }
  }

}
