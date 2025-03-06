package org.wisp.stream.iterator

import org.wisp.ActorLink

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure

abstract class SourceActorLink(using executor: ExecutionContext) extends StreamActorLink, ActorLink{

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
