package org.wisp

import scala.annotation.targetName

object using {

  extension [T <: AutoCloseable](ac: T) {
    @targetName("withClose")
    inline def | [R](inline f: T => R): R = {
      try {
        f.apply(ac)
      } finally {
        ac.close()
      }
    }
  }

  class UsingManager extends AutoCloseable{
    private var toClose:List[? <: AutoCloseable] = Nil

    def apply[T <: AutoCloseable](t:T):T = {
      toClose = t :: toClose
      t
    }

    override def close(): Unit = {
      for(c <- toClose) c.close()
      toClose = Nil
    }
  }

  inline def using[R](inline f: UsingManager => R): R = {
    val m = UsingManager()
    try {
      f.apply(m)
    } finally {
      m.close()
    }
  }

}
