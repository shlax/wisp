package org.wisp.stream.iterator

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Failure

trait SourceFlow[T](using ExecutionContextExecutor) extends StreamFlow[T]{
  
  def failOn(e: Throwable):this.type

  def failOn(p:Future[?]):this.type = {
    p.onComplete{
      case Failure(t) =>
        failOn(t)
      case _ =>
    }
    this
  }

}
