package org.wisp.stream

import org.wisp.Consumer

object Sink {

  /**
   * Creates [[Sink]] from function.
   *
   * Ignores `None` values
   */
  def apply[T](fn: T => Unit): Sink[T] = {
    (t: Option[T]) => {
      if(t.isDefined){
        fn.apply(t.get)
      }
    }
  }

}

/**
 * Is a [[Consumer]] of [[Option]]
 *
 * `Some(value)` is a value to be processed
 *
 * `None` indicates end of stream
 */
@FunctionalInterface
trait Sink[-T] extends Consumer[Option[T]]{

  def accept(t: T): Unit = {
    apply(Some(t))
  }

  /**
   * Indicates end of stream
   */
  def complete(): Unit = {
    apply(None)
  }

  def mapValues[R](fn: R => T): Sink[R] = {
    val self = this
    new Sink[R] {
      override def apply(e: Option[R]): Unit = {
        self.apply(e.map(fn))
      }

    }
  }

  def flatMapValues[R](fn: (R, this.type) => Unit): Sink[R] = {
    val self:this.type = this
    new Sink[R] {
      override def apply(e: Option[R]): Unit = {
        if(e.isDefined){
          fn.apply(e.get, self)
        }else{
          self.apply(None)
        }
      }
    }
  }

  def filterValues[R <: T](fn: R => Boolean): Sink[R] = {
    val self = this
    new Sink[R] {
      override def apply(e: Option[R]): Unit = {
        if(e.isDefined) {
          if (fn.apply(e.get)) self.apply(e)
        }else{
          self.apply(None)
        }
      }
    }
  }

  def collectValues[R](fn: PartialFunction[R, T]): Sink[R] = {
    val self = this
    new Sink[R] {
      override def apply(e: Option[R]): Unit = {
        if(e.isDefined) {
          val v = e.get
          if (fn.isDefinedAt(v)){
            val u = fn.apply(v)
            self.apply(Some(u))
          }
        }else{
          self.apply(None)
        }
      }

    }
  }

  /**
   * Consume [[org.wisp.stream.Source]] and call `complete`
   */
  def consume(s:Source[T]):Unit = {
    var v = s.next()
    while(v.isDefined){
      apply(v)
      v = s.next()
    }
    complete()
  }


}
