package org.wisp.stream.iterator

import org.wisp.stream.Sink
import org.wisp.ActorLink
import org.wisp.lock.*
import org.wisp.stream.iterator.message.*

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/** for each element of `stream` `sink.accept(...)` is called */
class StreamSink[T](stream :ActorLink, sink:Sink[T])(using ExecutionContext) extends StreamActorLink{

  protected val completed:Promise[Unit] = Promise()
  protected var started:Boolean = false

  /** start precessing data */
  def start(): Future[Unit] = lock.withLock{
    if(started){
      throw new IllegalStateException("started")
    }else{
      started = true
    }

    stream.call(HasNext).onComplete(accept)
    completed.future
  }

  override def accept(from: ActorLink): PartialFunction[Operation, Unit] = {

    case Next(v) =>
      if(completed.isCompleted) throw new IllegalStateException("ended")
      try {
        sink.accept(v.asInstanceOf[T])
        stream.call(HasNext).onComplete(accept)
      }catch{
        case NonFatal(exc) =>
          completed.failure(exc)
      }

    case End(ex) =>
      var err = ex

      if(err.isEmpty) {
        try {
          sink.complete()
        } catch {
          case NonFatal(exc) =>
            err = Some(exc)
        }
      }
      
      if(err.isEmpty){
        val c = completed.trySuccess(())
        if (!c) throw new IllegalStateException("ended")
      }else {
        completed.failure(err.get)
      }

  }

}
