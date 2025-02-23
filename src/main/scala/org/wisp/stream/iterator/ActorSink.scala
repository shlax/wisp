package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.*

import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import java.util.function.{BiConsumer, Consumer}

object ActorSink {

  def apply(prev: ActorLink, f: Consumer[Any]): ActorSink = {
    new ActorSink(prev, (_, m) => f.accept(m) )
  }

  def apply(prev: ActorLink, f: BiConsumer[ActorLink, Any]): ActorSink = {
    new ActorSink(prev, f)
  }

}

class ActorSink(prev:ActorLink, fn:BiConsumer[ActorLink, Any]) extends Consumer[Message]{

  private val completed = CompletableFuture[Void]
  private val lock = new ReentrantLock()

  def start(): CompletableFuture[Void] = {
    next(prev)
    completed
  }

  private def next(p:ActorLink): Unit = {
    p.ask(HasNext).thenAccept(this)
  }

  override def accept(t: Message): Unit = {
    lock.lock()
    try {
      t.message match {
        case Next(v) =>
          if (completed.isDone) throw new IllegalStateException("all ended")
          fn.accept(t.sender, v)
          next(t.sender)
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
