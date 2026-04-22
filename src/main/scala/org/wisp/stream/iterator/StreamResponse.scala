package org.wisp.stream.iterator

import org.wisp.Message
import org.wisp.utils.lock.*
import scala.util.{Failure, Success, Try}

trait StreamResponse[T] extends StreamLock{

  protected def accept: PartialFunction[Response[T], Unit]

  protected def accept(t: Try[Message[Response[T]]]): Unit = lock.withLock {
    t match {
      case Success(message) =>
        message.process(StreamResponse.this.getClass) {
          accept.apply(message.value)
        }
      case Failure(exception) =>
        throw exception
    }
  }

}
