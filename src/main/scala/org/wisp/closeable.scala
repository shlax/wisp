package org.wisp

import scala.annotation.targetName
import scala.util.control.NonFatal

/** A utility for management of [[AutoCloseable]] */
object closeable {

  extension [T <: AutoCloseable](ac: T) {

    /** support for java try-with-resources construct */
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

    /** Register [[AutoCloseable]] with this manager */
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

  /**
   * Example:
   * {{{
   * import org.wisp.closeable.*
   *
   * using{ use =>
   *   val in = use(new FileInputStream(...))
   *   val out = use(new FileOutputStream(...))
   *   ...
   * }
   * }}}
   */
  def using[R](f: UsingManager => R): R = {
    UsingManager() | { m =>
      f.apply(m)
    }
  }

}
