package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.*

import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

class ActorSink(prev:ActorLink, sink:Consumer[Any]) extends Consumer[Message]{

  private val completed = CompletableFuture[Void]
  private val lock = new ReentrantLock()

  def start(): CompletableFuture[Void] = {
    prev.ask(HasNext).thenAccept(this)
    completed
  }

  override def accept(t: Message): Unit = {
    lock.lock()
    try {
      t.message match {
        case Next(v) =>
          if (completed.isDone) throw new IllegalStateException("all ended")
          sink.accept(v)
          prev.ask(HasNext).thenAccept(this)
        case End =>
          if (!completed.complete(null)) {
            throw new IllegalStateException("all ended")
          }
      }
    }finally {
      lock.unlock()
    }
  }

}
