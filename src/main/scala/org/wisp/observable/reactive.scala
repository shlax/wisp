package org.wisp.observable

import org.wisp.utils.lock.withLock
import java.util.concurrent.locks.ReentrantLock

/**
 * The `reactive` object provides a mechanism to create observables that react to changes in their dependencies.
 * {{{
 * val a = Observable[Int](); val b = Observable[Int]()
 *
 * reactive[Int] { rx =>
 *   val va = rx(a) // register Observable as Variable
 *   val vb = rx(b) // register Observable as Variable
 *   rx{ !va + !vb } // register function
 * }.to(println)
 *
 * a(1); b(2)
 * }}}
 */
object reactive {

  /**
   * Defining reactive computations.
   */
  class Builder[T] {

    private[reactive] val observable: Observable[T] = Observable[T]()
    private[reactive] var variables: List[Variable[?]] = Nil
    private[reactive] var fn: Option[() => T] = None

    private var value : Option[T] = None

    private def trigger(): Unit = {
      if(variables.forall(_.value.isDefined)){
        val r = fn.get.apply()
        if(!value.contains(r)){
          value = Some(r)
          observable.apply(r)
        }
      }
    }

    protected def subscribe[V](o: Observable[V], v: Variable[V]): Observable[V]#Subscription = {
      o.to(v.set)
    }

    /**
     * Holder for last observed value
     */
    class Variable[V](o: Observable[V]) {
      val subscription: Observable[V]#Subscription = subscribe(o, this)

      private[reactive] var value:Option[V] = None

      private[reactive] def set(v:V): Unit = {
        if (!value.contains(v)) {
          value = Some(v)
          trigger()
        }
      }

      /**
       * get last observed value
       */
      def unary_! : V = {
        value.get
      }

    }

    /**
     * Register Observable as Variable
     */
    def apply[V](o: Observable[V]): Variable[V] = {
      if (fn.isDefined) {
        throw new IllegalStateException("Function is already defined")
      }
      val v = new Variable[V](o)
      variables = v :: variables
      v
    }

    /**
     * Register `function`
     *
     * `function` will be called when any variable is changed.
     *
     * `function` wil not be called before all variables are set.
     */
    def apply(function: => T): Observable[T] = {
      if(fn.isDefined){
        throw new IllegalStateException("Function is already defined")
      }
      fn = Some(() => { function })
      observable
    }

  }

  private class SynchronizedBuilder[T] extends Builder[T] {
    private val lock = new ReentrantLock()

    override protected def subscribe[V](o: Observable[V], v: Variable[V]): Observable[V]#Subscription = {
      o.to{ x =>
        lock.withLock{
          v.set(x)
        }
      }
    }
  }

  def apply[T](function: Builder[T] => Observable[T]): Observable[T] = {
    apply(false)(function)
  }

  /**
   * Returned [[Observable]] will be triggered only when the value is changed.
   * @return [[Observable]] computed by function defined in [[Builder]].
   */
  def apply[T](synchronized: Boolean)(function: Builder[T] => Observable[T]): Observable[T] = {
    val b = if(synchronized) new SynchronizedBuilder[T] else new Builder[T]
    val res = function(b)
    if (b.variables.isEmpty) {
      throw new IllegalStateException("Variables are not defined")
    }
    if (b.fn.isEmpty) {
      throw new IllegalStateException("Function is not defined")
    }
    res
  }

}
