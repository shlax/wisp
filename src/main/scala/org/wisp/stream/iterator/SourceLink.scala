package org.wisp.stream.iterator

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

trait SourceLink[T](using ExecutionContext) extends StreamLink[T]{

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
