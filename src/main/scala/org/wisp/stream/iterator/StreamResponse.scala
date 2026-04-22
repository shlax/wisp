package org.wisp.stream.iterator

import org.wisp.Message
import org.wisp.utils.lock.*

import java.util.concurrent.locks.ReentrantLock
import scala.util.{Failure, Success, Try}


class StreamResponse[T](override val lock:ReentrantLock)(fn: PartialFunction[Response[T], Unit]) extends StreamLock, (Try[Message[Response[T]]] => Unit) {

  override def apply(t: Try[Message[Response[T]]]): Unit = lock.withLock {
    t match {
      case Success(message) =>
        message.process(StreamResponse.this.getClass) {
          fn.apply(message.value)
        }
      case Failure(exception) =>
        throw exception
    }
  }

}
