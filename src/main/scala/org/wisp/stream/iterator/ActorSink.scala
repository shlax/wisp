package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.*

import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import java.util.function.{BiConsumer, Consumer}

object ActorSink {

  def apply(prev: Seq[ActorLink], f: Consumer[Any]): ActorSink = {
    new ActorSink(prev, (_, m) => f.accept(m) )
  }

  def apply(prev: ActorLink, f: Consumer[Any]): ActorSink = {
    new ActorSink(Seq(prev), (_, m) => f.accept(m) )
  }

  def apply(prev: Seq[ActorLink], f: BiConsumer[ActorLink, Any]): ActorSink = {
    new ActorSink(prev, f)
  }

  def apply(prev: ActorLink, f: BiConsumer[ActorLink, Any]): ActorSink = {
    new ActorSink(Seq(prev), f)
  }

}

class ActorSink(prev:Seq[ActorLink], fn:BiConsumer[ActorLink, Any]) extends Consumer[Message]{

  private val ended = Array.fill(prev.length)(false)
  private val completed = CompletableFuture[Void]

  private val lock = new ReentrantLock()

  def start(): CompletableFuture[Void] = {
    for(p <- prev) next(p)
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
          val ind = prev.indexOf(t.sender)
          if (ended(ind)) {
            if (completed.isDone) throw new IllegalStateException("all ended")
            throw new IllegalStateException("ended")
          }

          fn.accept(t.sender, v)
          next(t.sender)
        case End =>
          val ind = prev.indexOf(t.sender)
          if (ended(ind)) throw new IllegalStateException("ended")

          ended(ind) = true
          if (!ended.contains(false)) {
            if (!completed.complete(null)) {
              throw new IllegalStateException("all ended")
            }
          }
      }
    }finally {
      lock.unlock()
    }
  }

}
