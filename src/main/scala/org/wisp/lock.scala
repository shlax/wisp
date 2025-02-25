package org.wisp

import java.util.concurrent.locks.ReentrantLock

object lock {

  extension (l: ReentrantLock) {
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
