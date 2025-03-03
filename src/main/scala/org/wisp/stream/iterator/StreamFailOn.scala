package org.wisp.stream.iterator

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure

trait StreamFailOn {

  def fail(e: Throwable):this.type

  def failOn(p:Promise[?])(using executor: ExecutionContext):this.type = {
    failOn(p.future)
  }

  def failOn(p:Future[?])(using executor: ExecutionContext):this.type = {
    p.onComplete{
      case Failure(t) =>
        fail(t)
      case _ =>
    }
    this
  }

}
