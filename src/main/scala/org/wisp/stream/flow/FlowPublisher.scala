package org.wisp.stream.flow

import org.wisp.stream.iterator.{End, Next, Response, StreamFlow, StreamLink}

import java.util.concurrent.Flow
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContextExecutor
import org.wisp.utils.lock.*

import scala.util.Try
import scala.util.control.NonFatal

/**
 * provides interoperability with [[java.util.concurrent.Flow.Publisher]]
 */
class FlowPublisher[T](link:StreamFlow[T])(using ExecutionContextExecutor) extends Flow.Publisher[T]{

  protected class LinkSubscription(subscriber: Flow.Subscriber[? >: T]) extends Flow.Subscription, StreamLink[T] {
    protected override val lock = ReentrantLock()
    protected var canceled: Boolean = false

    protected var requested: Boolean = false
    protected var toRequest: Long = 0
    
    override protected def applyWithLock(rv: Response[T]): Unit = {
      try {
        rv match {
          case Next(t) =>
            try {
              if (!canceled) {
                subscriber.onNext(t)
              }
            } finally {
              requested = false
              pullNext()
            }
          case End =>
            if (!canceled) {
              subscriber.onComplete()
            }
        }
      } catch {
        case NonFatal(e) =>
          onError(e)
      }
    }

    protected def pullNext():Unit = {
      if(!requested && toRequest > 0){
        if(toRequest != Long.MaxValue) {
          toRequest -= 1
        }
        requested = true
        link.next(LinkSubscription.this)
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
        if(n == Long.MaxValue) {
          toRequest = Long.MaxValue
        }else{
          toRequest += n
        }
        pullNext()
      }
    }

    override def cancel(): Unit = lock.withLock {
      canceled = true
    }
  }

  override def subscribe(subscriber: Flow.Subscriber[? >: T]): Unit = {
    if(subscriber == null) throw new NullPointerException("subscriber is null")
    subscriber.onSubscribe(new LinkSubscription(subscriber))
  }

}
