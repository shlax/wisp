package org.qwActor.stream.iterator

import java.lang
import java.util

object Source{

  implicit class SourceIterator[E](i: util.Iterator[E]) extends Source[E] {
    override def next(): Option[E] = if(i.hasNext) Some(i.next()) else None
  }

  implicit class SourceIterable[E](it: lang.Iterable[E]) extends Source[E] {
    private val i = it.iterator()
    override def next(): Option[E] = if(i.hasNext) Some(i.next()) else None
  }

  implicit class SourceMap[K, V](it: util.Map[K, V]) extends Source[util.Map.Entry[K, V]] {
    private val i = it.entrySet().iterator()
    override def next(): Option[util.Map.Entry[K, V]] = if(i.hasNext) Some(i.next()) else None
  }

  implicit class SourceIterableOnce[E](it: IterableOnce[E]) extends Source[E] {
    private val i = it.iterator
    override def next(): Option[E] = if(i.hasNext) Some(i.next()) else None
  }

}

trait Source[T] {

  /** {{{if(hasNext) Some(next()) else None}}} */
  def next():Option[T]

}
