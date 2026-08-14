package org.wisp.stream.iterator

import org.wisp.utils.lock.*

trait StreamHandler[T] extends (Response[T] => Unit), StreamLock {

  /**
   * method is running with lock
   */
  protected def applyWithLock(from:Response[T]): Unit

  override def apply(t: Response[T]): Unit = lock.withLock{
    applyWithLock(t)
  }
  
}
