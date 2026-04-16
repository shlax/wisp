package org.wisp.utils

import scala.annotation.targetName
import scala.util.control.NonFatal

/**
 * A utility for management of [[java.lang.AutoCloseable]]
 */
object closeable {

  extension [T <: AutoCloseable](ac: T) {

    /**
     * support for java try-with-resources construct
     *
     * {{{
     * import org.wisp.closeable.*
     *
     * new FileInputStream(...) | { in =>
     *   ...
     * }
     * }}}
     */
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

  class UsingManager(from: List[? <: AutoCloseable] = Nil) extends AutoCloseable{
    private var toClose:List[? <: AutoCloseable] = from.reverse

    /**
     * Register [[java.lang.AutoCloseable]] with this manager
     */
    def apply[T <: AutoCloseable](t:T):T = {
      toClose = t :: toClose
      t
    }

    /**
     * Close all registered [[java.lang.AutoCloseable]] in reverse order of registration
     */
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

  /**
   * same as [[using]] but initialize [[UsingManager]] with `init` values
   */
  def using[R](init: AutoCloseable*)(f: UsingManager => R): R = {
    UsingManager(init.toList) | { m =>
      f.apply(m)
    }
  }

}
