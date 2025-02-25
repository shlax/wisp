package org.wisp.stream.iterator

import org.wisp.exceptions.ExceptionHandler
import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class StreamSink[T](eh:ExceptionHandler, prev:ActorLink, sink:Consumer[T]) extends StreamActorLink{

  protected val completed:CompletableFuture[Void] = CompletableFuture[Void]

  def start(): CompletableFuture[Void] = {
    prev.ask(HasNext).whenComplete(eh >> this)
    completed
  }

  override def accept(from: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case Next(v) =>
      if (completed.isDone) throw new IllegalStateException("ended")
      sink.accept(v.asInstanceOf[T])
      prev.ask(HasNext).whenComplete(eh >> this)
    case End =>
      if (!completed.complete(null)) {
        throw new IllegalStateException("ended")
      }
  }

}
