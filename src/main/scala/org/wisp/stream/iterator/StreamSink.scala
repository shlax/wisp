package org.wisp.stream.iterator

import org.wisp.{ActorMessage, ActorRef}

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

object StreamSink{

  def apply(prev:ActorRef)(fn:Consumer[Any]):StreamSink = new StreamSink(prev)(fn)

}

class StreamSink(prev:ActorRef)(fn:Consumer[Any]) extends ActorRef(prev.eventBus){
  private val cf = new CompletableFuture[Void]

  def start():CompletableFuture[Void] = {
    next()
    cf
  }

  private def next():Unit = {
    prev.ask(HasNext).thenAccept(this )
  }

  override def accept(t: ActorMessage): Unit = {
    t.value match {
      case Next(v) =>
        if(cf.isDone) throw new IllegalStateException("ended")
        fn.accept(v)
        next()

      case End =>
        cf.complete(null)
    }
  }
}