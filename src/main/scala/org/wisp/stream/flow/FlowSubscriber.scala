package org.wisp.stream.flow

import org.wisp.stream.iterator.{End, Next, Response, SingleNodeFlow, StreamFlow, StreamLink, SynchronizedFlow}
import org.wisp.utils.lock.withLock

import java.util
import java.util.concurrent.Flow
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext

/**
 * provides interoperability with [[java.util.concurrent.Flow.Subscriber]]
 */
class FlowSubscriber[T](publisher: Flow.Publisher[T])(using ec: ExecutionContext) extends SynchronizedFlow[T], SingleNodeFlow[T], Flow.Subscriber[T] {
  override protected val lock: ReentrantLock = ReentrantLock()
  override protected val nodes: util.Queue[Response[T] => Unit] = createNodes()

  protected var subscription:Option[Flow.Subscription] = None
  protected var requestedCount = 0
  protected var ended = false

  publisher.subscribe(this)

  protected def requestNext():Unit = {
    if(requestedCount == 0 && !ended && nodes.size() > 0){
      for(s <- subscription){
        requestedCount = nodes.size()
        s.request(requestedCount)
      }
    }
  }

  override def onSubscribe(s: Flow.Subscription): Unit = lock.withLock{
    if(s == null) throw new NullPointerException("subscription is null")
    subscription = Some(s)
    requestNext()
  }

  override def onNext(item: T): Unit = lock.withLock{
    val n = nodes.poll()
    if(n == null) throw new IllegalStateException("no nodes")
    requestedCount -= 1
    if(requestedCount < 0){
      throw new IllegalStateException("requestedCount < 0")
    }
    n.apply(Next(item))
    if(requestedCount == 0) {
      requestNext()
    }
  }

  override def onComplete(): Unit = lock.withLock{
    ended = true
    sendEnd()
  }

  override def nextWithLock(from: Response[T] => Unit): Unit = {
      if(ended){
        from.apply(End)
      }else{
        nodes.add(from)
        requestNext()
      }
  }

  override def onError(throwable: Throwable): Unit = {
    ec.reportFailure(throwable)
  }

}
