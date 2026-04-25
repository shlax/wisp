package org.wisp.stream.flow

import org.wisp.stream.iterator.OperationLink

import java.util.concurrent.Flow
import scala.concurrent.ExecutionContext

/**
 * provides interoperability with [[java.util.concurrent.Flow.Processor]]
 */
class FlowProcessor[T, R](publisher: Flow.Publisher[T], map: FlowSubscriber[T] => OperationLink[R])(using ExecutionContext)
  extends Flow.Processor[T, R] {

  protected val flowSubscriber: FlowSubscriber[T] = FlowSubscriber[T](publisher)
  protected val flowPublisher: FlowPublisher[R] = FlowPublisher[R](map(flowSubscriber))

  override def subscribe(subscriber: Flow.Subscriber[? >: R]): Unit = {
    flowPublisher.subscribe(subscriber)
  }

  override def onSubscribe(subscription: Flow.Subscription): Unit = {
    flowSubscriber.onSubscribe(subscription)
  }

  override def onNext(item: T): Unit = {
    flowSubscriber.onNext(item)
  }

  override def onError(throwable: Throwable): Unit = {
    flowSubscriber.onError(throwable)
  }

  override def onComplete(): Unit = {
    flowSubscriber.onComplete()
  }

}
