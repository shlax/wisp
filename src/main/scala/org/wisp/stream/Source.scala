package org.wisp.stream

import org.wisp.Consumer

object Source{

  /** empty Source */
  val empty: Source[Nothing] = () => None

  /** Source containing only the specified object */
  def apply[T](value:T) : Source[T] = {
    new Source[T] {
      private var ended = false
      override def next(): Option[T] = {
        if (ended) None else {
          ended = true
          Some(value)
        }
      }
    }
  }

}

/** `Iterator` more suitable for messaging */
@FunctionalInterface
trait Source[+T]{

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

  def filter[E >: T](p: E => Boolean): Source[T] = {
    val self = this
    new Source[T]() {

      def next(): Option[T] = {
        var n = self.next()
        while (n.isDefined && !p.apply(n.get)){
          n = self.next()
        }
        n
      }

    }
  }

  def collect[E >: T, R](fn: PartialFunction[T, R]): Source[R] = {
    val self = this
    new Source[R]() {
      var end = false
      
      def next(): Option[R] = {
        var r:Option[R] = None
        while (!end && r.isEmpty){
          self.next() match {
            case Some(v) =>
              if(fn.isDefinedAt(v)){
                r = Some(fn.apply(v))
              }
            case None =>
              end = true
          }
        }
        r
      }

    }
  }
  
  def fold[E](start:E)(collectFn: (E, T) => E): E = {
    var s = start
    forEach{ i =>
      s = collectFn(s, i)
    }
    s
  }

  def forEach[E >: T](c: E => Unit):Unit = {
    var v = next()
    while (v.isDefined){
      c.apply(v.get)
      v = next()
    }
  }

}
