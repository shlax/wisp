package org.wisp.stream.iterator

import java.util.concurrent.locks.ReentrantLock

trait StreamLock {

  protected val lock: ReentrantLock

}
