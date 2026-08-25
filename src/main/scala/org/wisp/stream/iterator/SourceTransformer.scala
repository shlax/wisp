package org.wisp.stream.iterator

import org.wisp.stream.Source

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

trait SourceTransformer[F, T] (using ec: ExecutionContext){

  protected val collect: Option[F] => Source[T]

  protected def call(value: Option[F]): Option[Source[T]] = {
    var opt: Option[Source[T]] = None
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
