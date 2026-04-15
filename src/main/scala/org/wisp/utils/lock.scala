package org.wisp.utils

import java.util.concurrent.locks.ReentrantLock

object lock {

  extension (l: ReentrantLock) {

    /**
     * execute `block` with lock
     *
     * @param block code to be executed with lock
     * @return result of `block`
     */
    inline def withLock[R](inline block: => R): R = {
      l.lockInterruptibly()
      try {
        block
      } finally {
        l.unlock()
      }
    }
  }

}
