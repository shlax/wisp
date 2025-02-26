package org.wisp.stream

import java.{lang, util}
import java.util.function.{Consumer, Predicate}

object Source{

  extension[E](i: util.Iterator[E]){
    def asSource: Source[E] = { () =>
      if (i.hasNext) Some(i.next()) else None
    }
  }

  extension[E](i: util.Enumeration[E]){
    def asSource: Source[E] = { () =>
      if (i.hasMoreElements) Some(i.nextElement()) else None
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

  def map[R](f: T => R): Source[R] = {
    val self = this
    new Source[R](){
      def next():Option[R] = {
        self.next().map( i => f.apply(i) )
      }
    }
  }

  def flatMap[R](f: T => Source[R]): Source[R] = {
    val self = this
    new Source[R]() {
      var last:Option[Source[R]] = None
      var end = false

      def next(): Option[R] = {
        var r : Option[R] = None
        while(!end && r.isEmpty){
          if(last.isEmpty){
            self.next() match {
              case None =>
                end = true
              case Some(x) =>
                last = Some(f.apply(x))
            }
          }
          for(q <- last){
            r = q.next()
            if(r.isEmpty) last = None
          }
        }
        r
      }
    }
  }

  def filter[E >: T](p:Predicate[E]): Source[T] = {
    val self = this
    new Source[T]() {
      def next(): Option[T] = {
        var n = self.next()
        while (n.isDefined && !p.test(n.get)){
          n = self.next()
        }
        n
      }
    }
  }

  def groupBy[K, E](keyFn: T => K, collectFn: (Option[E], T) => E): Source[E] = {
    val self = this
    new Source[E]() {
      private var lastValue: Option[E] = None
      private var lastKey: Option[K] = None
      private var end = false

      def next(): Option[E] = {
        var r:Option[E] = None
        while (!end && r.isEmpty){
          self.next() match{
            case Some(nv) =>
              val nk = keyFn.apply(nv)
              if(lastKey.contains(nk)){
                lastValue = Some(collectFn.apply(lastValue, nv))
              }else{
                r = lastValue
                lastValue = Some(collectFn.apply(None, nv))
                lastKey = Some(nk)
              }
            case None =>
              r = lastValue
              lastValue = None
              lastKey = None
              end = true
          }
        }
        r
      }
    }
  }

  def forEach[E >: T](c: Consumer[E]):Unit = {
    var v = next()
    while (v.isDefined){
      c.accept(v.get)
      v = next()
    }
  }

}
