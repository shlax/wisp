package org.wisp.stream.iterator

import org.wisp.utils.lock.*

trait StreamLink[T] extends OperationLink[T], StreamLock {

  /**
   * method is running with lock
   */
  def apply(from:OperationLink[T]): PartialFunction[Operation[T], Unit]

  override def apply(t: OperationMessage[T]): Unit = lock.withLock{
    t.process(StreamLink.this.getClass) {
      val f = apply(t.sender)
      f.apply(t.value)
    }
  }
  
}
