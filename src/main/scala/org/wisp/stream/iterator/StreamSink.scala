package org.wisp.stream.iterator

import org.wisp.stream.Sink
import org.wisp.utils.lock.*
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.control.NonFatal

/**
 * for each element of `stream` `sink.apply(...)` is called
 */
class StreamSink[T](stream :StreamFlow[T], override protected val sink:Sink[T])(using ExecutionContextExecutor) extends StreamHandler[T], SinkExecution[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()
  
  protected val completed:Promise[Unit] = Promise()
  protected var started:Boolean = false

  /**
   * start precessing data
   */
  def start: Future[Unit] = lock.withLock{
    if(started){
      throw new IllegalStateException("started")
    }else{
      started = true
    }

    stream.next(this)
    completed.future
  }

  protected var sinkException: Option[Throwable] = None

  protected override def onSinkException(t: Throwable): Unit = {
    sinkException = Some(t)
  }

  override def applyWithLock(value:Response[T]): Unit = value match {
    case Next(v) =>
      if(completed.isCompleted) throw new IllegalStateException("ended")

      tryApply(v)
      stream.next(this)

    case End =>
      var err:Option[Throwable] = None

      try {
        sink.complete()
      } catch {
        case NonFatal(exc) =>
          err = Some(exc)
      }

      if(err.isEmpty && sinkException.isEmpty){
        completed.success(())
      }else {
        if(err.isDefined) {
          completed.failure(err.get)
        }else{
          completed.failure(sinkException.get)
        }
      }

  }

}
