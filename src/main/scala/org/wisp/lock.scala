package org.wisp

import java.util.concurrent.locks.ReentrantLock

object lock {

  extension (l: ReentrantLock) {
    inline def withLock[R](inline block: => R): R = {
      l.lock()
      try {
        block
      } finally {
        l.unlock()
      }
    }
  }

}
