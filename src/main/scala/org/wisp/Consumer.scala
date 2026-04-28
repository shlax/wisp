package org.wisp

import org.wisp.stream.Source
import scala.util.{Failure, Success, Try}

object Consumer {

  /**
   * Creates [[Consumer]] from function
   */
  def apply[T](fn: T => Unit): Consumer[T] = {
    (t: T) => {
      fn.apply(t)
    }
  }

  /**
   * Creates [[Consumer]] that sends messages to [[Link]]
   */
  def apply[T](ref:Link[T, ?]):Consumer[T] = {
    (t: T) => {
      ref << t
    }
  }
  
}

/**
 * [[java.util.function.Consumer]] with added variance
 */
@FunctionalInterface
trait Consumer[-T] extends ( T => Unit ) {

  /**
   * [[java.util.function.Consumer#accept(java.lang.Object)]]
   */
  override def apply(t:T):Unit

  /**
   * {{{
   * val intConsumer : Consumer[Int] = (i:Int) => println(i + 3)
   * val stringConsumer : Consumer[String] = intConsumer.map(s => s.toInt)
   * stringConsumer( "120" ) // prints 123
   * }}}
   */
  def map[R](fn: R => T): Consumer[R] = {
    val self = this
    (e: R) => {
      self.apply(fn.apply(e))
    }
  }

  /**
   * {{{
   * val intConsumer : Consumer[Int] = (i:Int) => println(i + 3)
   * val stringConsumer = intConsumer.flatMap{ (s:String, c:Consumer[Int]) =>
   *   s.split(",").foreach( i => c(i.toInt) )
   * }
   * stringConsumer( "3,7" ) // prints 6 10
   * }}}
   */
  def flatMap[R](fn: (R, this.type) => Unit): Consumer[R] = {
    val self: this.type = this
    (e: R) => {
      fn.apply(e, self)
    }
  }

  /**
   * {{{
   * val intConsumer: Consumer[Int] = (i: Int) => println(i)
   * val filtered = intConsumer.filter(i => i % 2 == 0)
   * filtered(1) // prints nothing
   * filtered(2) // prints 2
   * }}}
   */
  def filter[R <: T](fn: R => Boolean): Consumer[R] = {
    val self = this
    (e: R) => {
      if (fn.apply(e)) self.apply(e)
    }
  }

  /**
   * Collects and converts values using [[scala.PartialFunction]]
   *
   * {{{
   * val intConsumer: Consumer[Int] = (i: Int) => println(i)
   * val anyConsumer: Consumer[Any] = intConsumer.collect{
   *   case s:String => s.toInt
   * }
   * anyConsumer("1") // prints 1
   * anyConsumer(Some(1)) // prints nothing
   * }}}
   */
  def collect[R](fn: PartialFunction[R, T]): Consumer[R] = {
    val self = this
    (e: R) => {
      if (fn.isDefinedAt(e)) self.apply(fn.apply(e))
    }
  }

  /**
   * [[java.util.function.Consumer#andThen(java.util.function.Consumer)]] with added variance
   */
  def andThen[S <: T](after: S => Unit): Consumer[S] = {
    val self = this
    (t: S) => {
      self.apply(t)
      after.apply(t)
    }
  }

  /**
   * Convert [[scala.util.Try]] to [[org.wisp.Consumer#apply]].
   *
   * [[scala.util.Failure]] will be thrown as `exception`
   */
  def apply(t: Try[T]): Unit = {
    t match {
      case Success(v) =>
          apply(v)
      case Failure(exception) =>
        throw exception
    }
  }

  /**
   * Consume [[org.wisp.stream.Source]]
   */
  def consume(s: Source[T]): Unit = {
    s.forEach(this)
  }

}
