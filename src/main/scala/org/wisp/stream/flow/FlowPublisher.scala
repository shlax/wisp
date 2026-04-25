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
    protected val lock = ReentrantLock()
    protected var canceled: Boolean = false

    protected var requested: Boolean = false
    protected var toRequest: Long = 0

    protected val response: StreamResponse[T] = new StreamResponse[T](lock, FlowPublisher.this.getClass)({
      case Next(t) =>
        try {
          if (!canceled) {
            subscriber.onNext(t)
          }
        }finally {
          requested = false
          pullNext()
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
            onError(e)
        }
      }
    }

    protected def pullNext():Unit = {
      if(!requested && toRequest > 0){
        toRequest -= 1
        requested = true
        link.call(HasNext).onComplete(response)
      }
    }

    protected def onError(e:Throwable):Unit = lock.withLock{
      try{
        if (!canceled) {
          subscriber.onError(e)
        }
      }finally {
        requested = false
        pullNext()
      }
    }

    override def request(n: Long): Unit = lock.withLock {
      if(n <= 0){
        subscriber.onError(new IllegalArgumentException("n must be positive"))
      }else {
        toRequest += n
        pullNext()
      }
    }

    override def cancel(): Unit = lock.withLock {
      canceled = true
    }
  }

  override def subscribe(subscriber: Flow.Subscriber[? >: T]): Unit = {
    subscriber.onSubscribe(new LinkSubscription(subscriber))
  }

}
