package org.wisp.stream.iterator

import org.wisp.ActorLink

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure

abstract class StreamFailOn(using executor: ExecutionContext) extends StreamActorLink, ActorLink, Runnable{

  def failOn(e: Throwable):this.type

  def failOn[T](p:Promise[T]):Future[T] = {
    val f = p.future
    failOn(f)
    f
  }

  def failOn(p:Future[?])(using executor: ExecutionContext):this.type = {
    p.onComplete{
      case Failure(t) =>
        failOn(t)
      case _ =>
    }
    this
  }

}
