package org.wisp.stream.iterator

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

trait SourceTransformer[F, T] (using ec: ExecutionContext){

  /**
   * Collects elements.
   * Will be called with `None` at the end of the stream.
   */
  protected val collect: Option[F] => () => Option[T]

  protected def call(value: Option[F]): Option[() => Option[T]] = {
    var opt: Option[() => Option[T]] = None
    try {
      val r = collect.apply(value)
      opt = Some(r)
    } catch {
      case NonFatal(ex) =>
        ec.reportFailure(ex)
    }
    opt
  }

}
