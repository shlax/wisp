package org.wisp.stream.iterator

import org.wisp.exceptions.UndeliveredException
import org.wisp.{ActorLink, Consumer, Message}
import org.wisp.stream.iterator.message.End

import scala.util.{Failure, Success, Try}

trait StreamException extends Consumer[Message]{

  def accept(t: Try[Message]): Unit = {
    t match{
      case Success(v) =>
        accept(v)
      case Failure(u) =>
        val end = End(Some(u))
        accept(Message( x => { throw UndeliveredException(x) }, end))
    }
  }

}
