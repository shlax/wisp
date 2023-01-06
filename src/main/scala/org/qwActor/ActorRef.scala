package org.qwActor

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.annotation.targetName

object ActorRef{
  val nil: ActorRef = m => {
    logger.warn("ignored message: "+m)
  }
}

trait ActorRef extends Consumer[ActorMessage]{
//  override def accept(t: ActorMessage): Unit

  @targetName("send")
  def << (value:Any) : Unit = {
    accept(ActorRef.nil, value)
  }

  def accept(r:ActorRef, m:Any): Unit = {
    accept(ActorMessage(r, m))
  }

  def ask(value: Any): CompletableFuture[ActorMessage] = {
    val f = new CompletableFuture[ActorMessage]
    accept(AskMessage((msg: ActorMessage) => {
      f.complete(msg)
    }, value, f))
    f
  }

}
