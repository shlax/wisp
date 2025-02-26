package org.wisp.stream.iterator

import org.wisp.stream.Sink
import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.control.NonFatal

class StreamSink[T](prev:ActorLink, sink:Sink[T]) extends StreamActorLink{

  protected val completed:CompletableFuture[Void] = CompletableFuture[Void]
  protected val sinkClosed = AtomicBoolean(false)

  def start(): CompletableFuture[Void] = {
    prev.ask(HasNext).whenComplete(this)
    completed
  }

  override def accept(from: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case Next(v) =>
      if(completed.isDone) throw new IllegalStateException("ended")
      try {
        sink.accept(v.asInstanceOf[T])
        prev.ask(HasNext).whenComplete(this)
      }catch{
        case NonFatal(exc) =>
          if(sinkClosed.compareAndSet(false, true)){
            completed.completeExceptionally(exc)
            autoClose(sink, Some(exc))
          }
      }
    case End(ex) =>
      var err = ex
      try{
        if(err.isEmpty){
          sink.flush()
        }
      }catch{
        case NonFatal(exc) =>
          err = Some(exc)
      }
      
      if(sinkClosed.compareAndSet(false, true)){
        try {
          autoClose(sink, err)
        } catch {
          case NonFatal(exc) =>
            err = Some(exc)
        }
      }

      if(err.isEmpty){
        val c = completed.complete(null)
        if (!c) throw new IllegalStateException("ended")
      }else if (sinkClosed.compareAndSet(false, true)) {
        completed.completeExceptionally(err.get)
      }
  }

}
