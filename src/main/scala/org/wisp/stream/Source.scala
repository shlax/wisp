package org.wisp.stream

object Source{

  /**
   * empty Source
   */
  val empty: Source[Nothing] = () => None

  /**
   * Source containing only the specified object
   */
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

/**
 * `Iterator` more suitable for messaging
 */
@FunctionalInterface
trait Source[+T]{

  /**
   * {{{if(hasNext) Some(next()) else None}}}
   */
  def next():Option[T]

  /**
   * Maps each element of the source stream to a new value using the provided `function`.
   */
  def map[R](function: T => R): Source[R] = {
    val self = this
    new Source[R](){

      def next():Option[R] = {
        self.next().map( i => function.apply(i) )
      }
      
    }
  }

  /**
   * Returns a new `Source` by applying a function to all elements of this `Source` and using the elements of the resulting `Source`.
   */
  def flatMap[R](function: T => Source[R]): Source[R] = {
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
                last = Some(function.apply(x))
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

  /**
   * Filters the elements of this `Source` using the specified `predicate`.
   */
  def filter[E >: T](predicate: E => Boolean): Source[T] = {
    val self = this
    new Source[T]() {

      def next(): Option[T] = {
        var n = self.next()
        while (n.isDefined && !predicate.apply(n.get)){
          n = self.next()
        }
        n
      }

    }
  }

  /**
   * Filters and converts values using [[scala.PartialFunction]]
   */
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

  /**
   * Folds the elements of this `Source` using the specified associative binary `operator`.
   * @return the result of applying the fold `operator` between `zero` and all the elements
   */
  def fold[E](zero:E)(operator: (E, T) => E): E = {
    var s = zero
    forEach{ i =>
      s = operator.apply(s, i)
    }
    s
  }

  /**
   * Calls `consumer` for each element of the source stream.
   */
  def forEach[E >: T](consumer: E => Unit):Unit = {
    var v = next()
    while (v.isDefined){
      consumer.apply(v.get)
      v = next()
    }
  }

}
