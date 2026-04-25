package org.wisp.stream.flow

import org.wisp.stream.iterator.{End, HasNext, Next, OperationLink, OperationMessage, StreamResponse}

import java.util.concurrent.Flow
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext
import org.wisp.utils.lock.*

import scala.util.Try
import scala.util.control.NonFatal

/**
 * provides interoperability with [[java.util.concurrent.Flow.Publisher]]
 */
class FlowPublisher[T](link:OperationLink[T])(using ExecutionContext) extends Flow.Publisher[T]{

  protected class LinkSubscription(subscriber: Flow.Subscriber[? >: T]) extends Flow.Subscription {
    private val lock = ReentrantLock()
    private var canceled: Boolean = false

    protected val response: StreamResponse[T] = new StreamResponse[T](lock, FlowPublisher.this.getClass)({
      case Next(t) =>
        if (!canceled) {
          subscriber.onNext(t)
        }
      case End =>
        if (!canceled) {
          subscriber.onComplete()
        }
    }) {
      override def apply(t: Try[OperationMessage[T]]): Unit = {
        try {
          super.apply(t)
        }catch {
          case NonFatal(e) =>
            subscriber.onError(e)
        }
      }
    }

    override def request(n: Long): Unit = lock.withLock {
      link.call(HasNext).onComplete(response)
    }

    override def cancel(): Unit = lock.withLock {
      canceled = true
    }
  }

  override def subscribe(subscriber: Flow.Subscriber[? >: T]): Unit = {
    subscriber.onSubscribe(new LinkSubscription(subscriber))
  }

}
