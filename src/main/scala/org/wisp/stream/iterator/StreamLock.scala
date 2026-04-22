package org.wisp.stream.iterator

import java.util.concurrent.locks.ReentrantLock

trait StreamLock {

  protected def lock: ReentrantLock

}
