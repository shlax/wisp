package org.wisp.observable

/**
 * The `reactive` object provides a mechanism to create observables that react to changes in their dependencies.
 * {{{
 * val a = Observable[Int]()
 * val b = Observable[Int]()
 * reactive[Int] { rx =>
 *   val va = rx(a) // register Observable as Variable
 *   val vb = rx(b) // register Observable as Variable
 *   rx{ va() + vb() } // register function
 * }.to(println)
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

    /**
     * Holder for last observed value
     */
    class Variable[V] {
      private[reactive] var value:Option[V] = None

      private[reactive] val setter : V => Unit = { v =>
        if (!value.contains(v)) {
          value = Some(v)
          trigger()
        }
      }

      /**
       * get last observed value
       */
      def apply(): V = {
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
      val v = new Variable[V]()
      variables = v :: variables
      o.to(v.setter)
      v
    }

    /**
     * Register `function`
     *
     * `function` will be called when any variable is changed.
     *
     * `function` wil not be called before all variables are set.
     */
    def apply(function: => T): Unit = {
      if(fn.isDefined){
        throw new IllegalStateException("Function is already defined")
      }
      fn = Some(() => { function })
    }

  }

  /**
   * Returned [[Observable]] will be triggered only when the value is changed.
   * @return [[Observable]] computed by function defined in [[Builder]].
   */
  def apply[T](fn: Builder[T] => Unit): Observable[T] = {
    val b = new Builder[T]
    fn(b)
    if (b.variables.isEmpty) {
      throw new IllegalStateException("Variables are not defined")
    }
    if (b.fn.isEmpty) {
      throw new IllegalStateException("Function is not defined")
    }
    b.observable
  }

}
