package org.wisp

import scala.annotation.targetName

object Using {

  extension [T <: AutoCloseable](ac: T) {
    @targetName("withClose")
    inline def | [R](f: T => R): R = {
      try {
        f.apply(ac)
      } finally {
        ac.close()
      }
    }
  }

  class UsingManager extends AutoCloseable{
    private var toClose:List[? <: AutoCloseable] = Nil

    inline def apply[T <: AutoCloseable](t:T):T = {
      toClose = t :: toClose
      t
    }

    inline override def close(): Unit = {
      for(c <- toClose) c.close()
    }
  }

  inline def using[R](f: UsingManager => R): R = {
    val m = new UsingManager
    try {
      f.apply(m)
    } finally {
      m.close()
    }
  }

}
