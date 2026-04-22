package org.wisp.stream.iterator

import org.wisp.utils.lock.*
import org.wisp.{Link, Message}

trait StreamLink[T] extends Link[Operation[T], Operation[T]], StreamLock {

  /**
   * method is running with lock
   */
  def apply(from:Link[Operation[T], Operation[T]]): PartialFunction[Operation[T], Unit]

  override def apply(t: Message[Operation[T], Operation[T]]): Unit = lock.withLock{
    t.process(StreamLink.this.getClass) {
      val f = apply(t.sender)
      f.apply(t.value)
    }
  }
  
}
