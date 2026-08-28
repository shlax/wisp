package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.utils.lock.*

import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContextExecutor
import scala.util.control.NonFatal

class StreamSource[T](src:Source[T])(using ec : ExecutionContextExecutor) extends SourceFlow[T], ExecutionFlow[T] {

  protected override val lock:ReentrantLock = new ReentrantLock()
  
  protected var ended = false

  protected var sourceException:Option[Throwable] = None

  override def failOn(e: Throwable): this.type = lock.withLock {
    sourceException = Some(e)
    this
  }

  override def nextWithLock(sender: Option[T] => Unit): Unit = {
    if (ended || sourceException.isDefined) {
      sender.apply(None)
    } else {

      var n: Option[T] = None
      try {
        n = src.next()
      } catch {
        case NonFatal(e) =>
          sourceException = Some(e)
          ec.reportFailure(e)
      }

      if (n.isDefined) {
        sender.apply(n)
      } else {
        ended = true
        sender.apply(None)
      }

    }
  }

}
