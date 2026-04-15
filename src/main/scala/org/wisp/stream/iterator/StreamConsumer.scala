package org.wisp.stream.iterator

import org.wisp.{Consumer, Message}

import scala.util.{Failure, Success, Try}

trait StreamConsumer extends Consumer[Message]{

  /**
   * Convert [[Try]] to stream [[Consumer.apply]].
   * [[Failure]] will be mapped to [[End]] with `exception`
   *
   * Example: `[ActorLink].call(HasNext).onComplete(apply)`
   */
  def apply(t: Try[Message]): Unit = {
    t match{
      case Success(v) =>
        apply(v)
      case Failure(u) =>
        throw u
    }
  }

}
