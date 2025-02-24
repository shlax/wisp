package org.wisp

import java.util.concurrent.locks.ReentrantLock

object lock {

  extension (l: ReentrantLock) {
    inline def withLock[R](inline f: => R): R = {
      l.lock()
      try {
        f
      } finally {
        l.unlock()
      }
    }
  }

}
