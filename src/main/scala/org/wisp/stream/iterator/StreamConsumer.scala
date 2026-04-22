package org.wisp.stream.iterator

import org.wisp.Message
import org.wisp.utils.lock.*
import scala.util.{Failure, Success, Try}

trait StreamConsumer[T] extends StreamLock{

  protected def accept : PartialFunction[Operation[T], Unit]

  protected def accept(t: Try[Message[Operation[T]]]): Unit = lock.withLock {
    t match {
      case Success(message) =>
        message.process(StreamConsumer.this.getClass) {
          accept.apply(message.value)
        }
      case Failure(exception) =>
        throw exception
    }
  }

}
