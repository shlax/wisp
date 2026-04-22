package org.wisp.stream.iterator

import org.wisp.Message
import org.wisp.utils.lock.*

import java.util.concurrent.locks.ReentrantLock
import scala.util.{Failure, Success, Try}

abstract class StreamResponse[T](override val lock:ReentrantLock) extends StreamLock, (Try[Message[Response[T]]] => Unit) {

  protected def accept: PartialFunction[Response[T], Unit]

  override def apply(t: Try[Message[Response[T]]]): Unit = lock.withLock {
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
