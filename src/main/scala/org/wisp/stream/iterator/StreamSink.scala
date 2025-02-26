package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.*

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.{BiConsumer, Consumer}
import scala.util.control.NonFatal

class StreamSink[T](prev:ActorLink, sink:Consumer[T]) extends StreamActorLink, BiConsumer[Message, Throwable]{

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
    case End =>
      var e: Throwable = null
      try{
        autoFlush(sink)
      }catch{
        case NonFatal(exc) =>
          e = exc
      }finally {
        if(sinkClosed.compareAndSet(false, true)){
          try {
            autoClose(sink, Option(e))
          } catch {
            case NonFatal(exc) =>
              e = exc
          }
        }
      }
      if(e == null){
        val c = completed.complete(null)
        if (!c) throw new IllegalStateException("ended")
      }else if (sinkClosed.compareAndSet(false, true)) {
        completed.completeExceptionally(e)
      }

  }

  override def accept(t: Message, u: Throwable): Unit = {
    if(u != null){
      if(sinkClosed.compareAndSet(false, true)){
        completed.completeExceptionally(u)
        autoClose(sink, Some(u))
      }
    }else{
      accept(t)
    }
  }

}
