package org.wisp.stream.iterator

import org.wisp.exceptions.UndeliveredException
import org.wisp.{ActorLink, Consumer, Message}
import org.wisp.stream.iterator.message.End

import scala.util.{Failure, Success, Try}

trait StreamConsumer extends Consumer[Message]{

  /** Convert [[Try]] to stream [[Consumer.accept]].
   * [[Failure]] will be mapped to [[End]] with `exception`
   *
   * Example: `[ActorLink].call(HasNext).onComplete(accept)` */
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
