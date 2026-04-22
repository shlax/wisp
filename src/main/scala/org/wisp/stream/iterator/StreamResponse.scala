package org.wisp.stream.iterator

import org.wisp.utils.lock.*

import java.util.concurrent.locks.ReentrantLock
import scala.util.{Failure, Success, Try}

class StreamResponse[T](override val lock:ReentrantLock)(fn: PartialFunction[Response[T], Unit]) extends StreamLock, (Try[OperationMessage[T]] => Unit) {

  override def apply(t: Try[OperationMessage[T]]): Unit = lock.withLock {
    t match {
      case Success(message) =>
        message.process(StreamResponse.this.getClass) {
          fn.apply(message.value.asInstanceOf[Response[T]])
        }
      case Failure(exception) =>
        throw exception
    }
  }

}
