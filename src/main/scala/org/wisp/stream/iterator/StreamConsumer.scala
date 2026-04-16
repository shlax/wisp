package org.wisp.stream.iterator

import org.wisp.{Consumer, Message}

import scala.util.{Failure, Success, Try}

trait StreamConsumer extends Consumer[Message]{

  /**
   * Convert [[scala.util.Try]] to stream [[org.wisp.Consumer#apply]].
   * [[scala.util.Failure]] will be thrown as `exception`
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
