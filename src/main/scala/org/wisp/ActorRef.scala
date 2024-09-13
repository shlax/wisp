package org.wisp

import org.wisp.bus.{EventBus, UndeliveredMessage}

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.annotation.targetName

object ActorRef{

  def nil(bus:EventBus): ActorRef = new ActorRef(bus) {
    override def accept(m: ActorMessage): Unit = {
      publish(new UndeliveredMessage(m))
    }
  }

}

abstract class ActorRef(val eventBus:EventBus) extends EventBus, Consumer[ActorMessage]{
  override def publish(event: Any): Unit = eventBus.publish(event)
  //  override def accept(t: ActorMessage): Unit

  @targetName("send")
  def << (value:Any) : Unit = {
    accept(ActorRef.nil(eventBus), value)
  }

  def ask(value: Any): CompletableFuture[ActorMessage] = {
    val f = new CompletableFuture[ActorMessage]
    accept(AskMessage(new ActorRef(eventBus) {
        override def accept(msg: ActorMessage): Unit = {
          f.complete(msg)
        }
      }, value, f))
    f
  }

  def accept(r: ActorRef, m: Any): Unit = {
    accept(ActorMessage(r, m))
  }

}
