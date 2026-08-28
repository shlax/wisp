package org.wisp.stream.iterator

import org.wisp.utils.lock.*

trait StreamHandler[T] extends (Option[T] => Unit), StreamLock {

  /**
   * method is running with lock
   */
  protected def applyWithLock(from:Option[T]): Unit

  override def apply(t: Option[T]): Unit = lock.withLock{
    applyWithLock(t)
  }
  
}
