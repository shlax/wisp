package org.wisp.stream.iterator

import org.wisp.stream.Sink
import org.wisp.Link
import org.wisp.utils.lock.*
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
 * for each element of `stream` `sink.apply(...)` is called
 */
class StreamSink[T](stream :Link[Operation[T], Operation[T]], override val sink:Sink[T])(using ExecutionContext) extends StreamLink[T], SinkExecution[T]{

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

    stream.call(HasNext).onComplete(apply)
    completed.future
  }

  protected var sinkException: Option[Throwable] = None

  protected override def onSinkException(t: Throwable): Unit = {
    sinkException = Some(t)
  }

  override def apply(from: Link[Operation[T], Operation[T]]): PartialFunction[Operation[T], Unit] = {

    case Next(v) =>
      if(completed.isCompleted) throw new IllegalStateException("ended")

      tryApply(v)
      stream.call(HasNext).onComplete(apply)

    case End =>
      var err:Option[Throwable] = None

      try {
        sink.complete()
      } catch {
        case NonFatal(exc) =>
          err = Some(exc)
      }

      if(err.isEmpty && sinkException.isEmpty){
        val c = completed.trySuccess(())
        if (!c) throw new IllegalStateException("ended")
      }else {
        if(err.isDefined) {
          completed.failure(err.get)
        }else{
          completed.failure(sinkException.get)
        }
      }

  }

}
