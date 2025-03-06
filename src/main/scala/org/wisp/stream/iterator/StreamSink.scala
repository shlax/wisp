package org.wisp.stream.iterator

import org.wisp.stream.Sink
import org.wisp.ActorLink
import org.wisp.lock.*
import org.wisp.stream.iterator.message.*

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

class StreamSink[T](prev:ActorLink, sink:Sink[T])(using executor: ExecutionContext) extends StreamActorLink{

  protected val completed:Promise[Unit] = Promise()
  protected var started:Boolean = false
  
  def start(): Future[Unit] = lock.withLock{
    if(started){
      throw new IllegalStateException("started")
    }else{
      started = true
    }

    prev.ask(HasNext).future.onComplete(accept)
    completed.future
  }

  override def accept(from: ActorLink): PartialFunction[IteratorMessage, Unit] = {

    case Next(v) =>
      if(completed.isCompleted) throw new IllegalStateException("ended")
      try {
        sink.accept(v.asInstanceOf[T])
        prev.ask(HasNext).future.onComplete(accept)
      }catch{
        case NonFatal(exc) =>
          completed.failure(exc)
      }

    case End(ex) =>
      var err = ex

      if(err.isEmpty) {
        try {
          sink.flush()
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
