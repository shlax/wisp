package org.qwActor.stream.iterator

import org.qwActor.stream.iterator.messages.{End, HasNext, Next}
import org.qwActor.{ActorMessage, ActorRef}

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

object ActorSink{

  def apply(prev:ActorRef)(fn:Consumer[Any]):ActorSink = new ActorSink(prev)(fn)

}

class ActorSink(prev:ActorRef)(fn:Consumer[Any]) extends ActorRef {
  private val cf = new CompletableFuture[Void]

  def start():CompletableFuture[Void] = {
    next()
    cf
  }

  private def next():Unit = {
    prev.accept(this, HasNext)
  }

  override def accept(t: ActorMessage): Unit = {
    t.value match {
      case Next(v) =>
        fn.accept(v)
        next()

      case End =>
        cf.complete(null)
    }
  }
}