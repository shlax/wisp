package org.wisp.stream

import org.wisp.utils.lock.withLock
import java.util.concurrent.locks.ReentrantLock
import java.util

object SinkSource {

  def unapply[T](s: SinkSource[T]): (Source[T], Sink[T]) = {
    (s.source, s.sink)
  }
}

/**
 * Provides a [[Source]] and [[Sink]] that can be used to communicate between different threads.
 * @param bufferSize maximum no of elements to buffer
 */
class SinkSource[T](bufferSize:Int = 1) {

  protected val lock = new ReentrantLock()
  protected val condition = lock.newCondition()

  protected var ended = false
  protected val queue:util.Queue[T] = createQueue(bufferSize)

  protected def createQueue(size:Int): util.Queue[T] = {
    new util.LinkedList[T]()
  }

  /**
   * Source that can be used to read elements from the sink.
   */
  val source:Source[T] = createSource()

  protected def createSource(): Source[T] = new Source[T] {
    override def next(): Option[T] = lock.withLock {
      while (queue.isEmpty && !ended) condition.await()
      val v = queue.poll()
      condition.signal()
      Option(v)
    }
  }

  /**
   * Sink that can be used to write elements to the source.
   */
  val sink:Sink[T] = createSink()

  protected def createSink(): Sink[T] = new Sink[T] {
    override def apply(t: T): Unit = lock.withLock {
      if(t == null) throw new NullPointerException()
      if(ended) throw new IllegalStateException("ended")
      while (queue.size() >= bufferSize) condition.await()
      queue.add(t)
      condition.signal()
    }

    override def complete(): Unit = lock.withLock {
      if (ended) throw new IllegalStateException("ended")
      while (!queue.isEmpty) condition.await()
      ended = true
      condition.signal()
    }
  }

}
