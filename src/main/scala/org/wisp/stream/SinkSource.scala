package org.wisp.stream

import org.wisp.utils.lock.withLock
import java.util.concurrent.locks.ReentrantLock

object SinkSource {

  def unapply[T](s: SinkSource[T]): (Source[T], Sink[T]) = {
    (s.source, s.sink)
  }
}

/**
 * Provides a [[Source]] and [[Sink]] that can be used to communicate between different threads.
 */
class SinkSource[T] {

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private var value:Option[T] = None
  private var ended = false

  /**
   * Source that can be used to read elements from the sink.
   */
  val source:Source[T] = new Source[T] {
    override def next(): Option[T] = lock.withLock {
      while (value.isEmpty && !ended) condition.await()
      val v = value
      value = None
      condition.signal()
      v
    }
  }

  /**
   * Sink that can be used to write elements to the source.
   */
  val sink:Sink[T] = new Sink[T] {
    override def apply(t: T): Unit = lock.withLock {
      if (ended) throw new IllegalStateException("ended")
      while (value.isDefined) condition.await()
      value = Some(t)
      condition.signal()
    }

    override def complete(): Unit = lock.withLock {
      if (ended) throw new IllegalStateException("ended")
      while (value.isDefined) condition.await()
      ended = true
      condition.signal()
    }
  }

}
