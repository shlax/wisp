package org.wisp.stream.iterator

import org.wisp.utils.lock.withLock

import scala.concurrent.ExecutionContextExecutor
import scala.util.control.NonFatal

trait ExecutionFlow[T] extends StreamFlow[T], StreamLock {

  protected def nextWithLock(callback: Option[T] => Unit): Unit

  override def next(callback: Option[T] => Unit)(using ec : ExecutionContextExecutor) : Unit = {
    ec.execute( () => {
      try {
        lock.withLock {
          nextWithLock { v =>
            ec.execute( () => {
              try {
                callback.apply(v)
              } catch {
                case NonFatal(e) =>
                  ec.reportFailure(e)
              }
            } )
          }
        }
      } catch {
        case NonFatal(e) =>
          ec.reportFailure(e)
      }
    } )
  }

}
