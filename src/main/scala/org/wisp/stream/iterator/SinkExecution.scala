package org.wisp.stream.iterator

import org.wisp.stream.Sink

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

trait SinkExecution[T](using executionContext: ExecutionContext) {

  protected val sink: Sink[T]

  protected def onSinkException(e:Throwable):Unit

  def tryApply(t: T): Unit = {
    try{
      sink.apply(Some(t))
    }catch {
      case NonFatal(e) =>
        onSinkException(e)
        executionContext.reportFailure(e)
    }
  }

  def complete(): Unit = {
    sink.complete()
  }

}
