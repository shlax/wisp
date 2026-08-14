package org.wisp.stream.iterator

import org.wisp.utils.lock.withLock

import java.util.concurrent.CompletableFuture
import scala.concurrent.ExecutionContextExecutor
import scala.util.control.NonFatal

trait SynchronizedFlow[T] extends StreamFlow[T], StreamLock {

  protected def nextWithLock(callback: Response[T] => Unit): Unit

  override def next(callback: Response[T] => Unit)(using ec : ExecutionContextExecutor) : Unit = {
    val cf = new CompletableFuture[Response[T]]()
    ec.execute( () => {
      try {
        lock.withLock {
          nextWithLock { v =>
            cf.complete(v)
          }
        }
      } catch {
        case NonFatal(e) =>
          cf.completeExceptionally(e)
      }
    } )
    cf.whenCompleteAsync( (v, e) => {
      if (e != null) {
        ec.reportFailure(e)
      }
      try {
        if(v != null){
          callback.apply(v)
        }
      } catch {
        case NonFatal(e) =>
          ec.reportFailure(e)
      }
    }, ec )
  }

}
