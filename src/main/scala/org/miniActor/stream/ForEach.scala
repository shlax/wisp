package org.miniActor.stream

import java.{lang, util}
import java.util.function.Consumer

object ForEach{

  implicit class ForEachIterator[E](i: util.Iterator[E]) extends ForEach[E]{
    override def forEach(c: Consumer[_ >: E]): Unit = {
      i.forEachRemaining(c)
    }
  }

  implicit class ForEachIterable[E](i: lang.Iterable[E]) extends ForEach[E]{
    override def forEach(c: Consumer[_ >: E]): Unit = {
      i.forEach(c)
    }
  }

  implicit class ForEachMap[K, V](i: util.Map[K, V]) extends ForEach[util.Map.Entry[K, V]]{
    override def forEach(c: Consumer[_ >: util.Map.Entry[K, V]]): Unit = {
      i.entrySet().forEach( e => c.accept(e) )
    }
  }

  implicit class ForEachIterableOnce[E](i: IterableOnce[E]) extends ForEach[E]{
    override def forEach(c: Consumer[_ >: E]): Unit = {
      val it = i.iterator
      while (it.hasNext) c.accept(it.next())
    }
  }

}

trait ForEach[T] {

  def forEach(c: Consumer[_ >: T]):Unit

}
