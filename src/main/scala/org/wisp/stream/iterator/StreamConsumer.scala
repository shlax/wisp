package org.wisp.stream.iterator

import org.wisp.{Consumer, Message}

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
        throw u
    }
  }

}
