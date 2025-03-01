package org.wisp

import scala.annotation.targetName
import scala.util.control.NonFatal

object using {

  extension [T <: AutoCloseable](ac: T) {

    /** support for javas try-with-resources construct */
    @targetName("autoClose")
    inline def | [R](inline f: T => R): R = {
      var e:Throwable = null
      try {
        f.apply(ac)
      }catch{
        case NonFatal(exc) =>
          e = exc
          throw e
      } finally {
        if(e == null){
          ac.close()
        }else {
          try {
            ac.close()
          } catch {
            case NonFatal(ex) =>
              e.addSuppressed(ex)
          }
        }
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

  def using[R](f: UsingManager => R): R = {
    UsingManager() | { m =>
      f.apply(m)
    }
  }

}
