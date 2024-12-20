package org.wisp.stream.iterator

import org.wisp.{ActorRef, Message}

import java.util.concurrent.CompletableFuture
import java.util.function.{BiConsumer, Consumer}

class ActorSink(prev:ActorRef)(fn:BiConsumer[ActorRef, Any]) extends ActorRef(prev.system){

  def this(prev:ActorRef)(f:Consumer[Any]) = {
    this(prev)( (_, m) => { f.accept(m) } )
  }

  private val cf = new CompletableFuture[Void]

  def start(): CompletableFuture[Void] = {
    next()
    cf
  }

  private def next(): Unit = {
    prev.ask(HasNext).thenAccept(this)
  }

  override def accept(t: Message): Unit = {
    t.message match {
      case Next(v) =>
        if(cf.isDone) throw new IllegalStateException("ended")
        fn.accept(t.from, v)
        next()
      case End =>
        cf.complete(null)
    }
  }

}
