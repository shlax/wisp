package org.wisp.stream.iterator

import org.wisp.{ActorRef, Message}
import org.wisp.stream.iterator.message.*

import java.util.concurrent.CompletableFuture
import java.util.function.{BiConsumer, Consumer}

object ActorSink {

  def apply(prev: Seq[ActorRef], f: Consumer[Any]): ActorSink = {
    new ActorSink(prev, (_, m) => f.accept(m) )
  }

  def apply(prev: ActorRef, f: Consumer[Any]): ActorSink = {
    new ActorSink(Seq(prev), (_, m) => f.accept(m) )
  }

  def apply(prev: Seq[ActorRef], f: BiConsumer[ActorRef, Any]): ActorSink = {
    new ActorSink(prev, f)
  }

  def apply(prev: ActorRef, f: BiConsumer[ActorRef, Any]): ActorSink = {
    new ActorSink(Seq(prev), f)
  }

}

class ActorSink(prev:Seq[ActorRef], fn:BiConsumer[ActorRef, Any]) extends Consumer[Message]{

  private val cfs = Array.fill(prev.length)(false)
  private val all = CompletableFuture[Void]

  def start(): CompletableFuture[Void] = {
    for(p <- prev) next(p)
    all
  }

  private def next(p:ActorRef): Unit = {
    p.ask(HasNext).thenAccept(this)
  }

  override def accept(t: Message): Unit = {
    t.message match {
      case Next(v) =>
        val ind = prev.indexOf(t.sender)
        if(cfs(ind)){
          if(all.isDone) throw new IllegalStateException("all ended")
          throw new IllegalStateException("ended")
        }

        fn.accept(t.sender, v)
        next(t.sender)
      case End =>
        val ind = prev.indexOf(t.sender)
        if(cfs(ind)) throw new IllegalStateException("ended")
        cfs(ind) = true
        if(!cfs.contains(false)){
          if(!all.complete(null)){
            throw new IllegalStateException("all ended")
          }
        }
    }
  }

}
