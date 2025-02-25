package org.wisp

import scala.annotation.targetName
import scala.util.control.NonFatal

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
      var e:Option[Throwable] = None
      for (i <- toClose){
          try {
            i.close()
          }catch {
            case NonFatal(ex) =>
              if(e.isDefined) ex.addSuppressed(e.get)
              e = Some(ex)
          }
      }
      toClose = Nil
      if(e.isDefined){
        throw e.get
      }
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
