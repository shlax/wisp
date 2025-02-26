package org.wisp

import scala.annotation.targetName
import scala.util.control.NonFatal

object using {

  extension [T <: AutoCloseable](ac: T) {
    @targetName("withClose")
    inline def | [R](inline f: T => R): R = {
      var r:Option[R] = None
      var e:Throwable = null
      try {
        val x = f.apply(ac)
        r = Some(x)
      }catch{
        case NonFatal(ex) =>
          e = ex
      } finally {
        try {
          ac.close()
        }catch{
          case NonFatal(ex) =>
            if(e != null) e.addSuppressed(ex)
            else e = ex
        }
      }
      if (e != null) {
        throw e
      }
      r.get
    }
  }

  class UsingManager extends AutoCloseable{
    private var toClose:List[? <: AutoCloseable] = Nil

    def apply[T <: AutoCloseable](t:T):T = {
      toClose = t :: toClose
      t
    }

    override def close(): Unit = {
      var e:Throwable = null
      for (i <- toClose){
          try {
            i.close()
          }catch {
            case NonFatal(ex) =>
              if(e != null) ex.addSuppressed(e)
              e = ex
          }
      }
      toClose = Nil
      if(e != null){
        throw e
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
