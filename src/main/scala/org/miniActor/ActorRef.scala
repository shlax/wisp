package org.miniActor

import org.miniActor.jfr.UndeliverableMessage

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.annotation.targetName

object ActorRef{
  val nil: ActorRef = m => {
    val e = new UndeliverableMessage
    if (e.isEnabled && e.shouldCommit) {
      e.message = m.toString
      e.commit()
    }
    if(logger.isWarnEnabled) logger.warn("ignored message: "+m)
  }
}

trait ActorRef extends Consumer[ActorMessage]{
//  override def accept(t: ActorMessage): Unit

  @targetName("send")
  def << (value:Any) : Unit = {
    accept(ActorRef.nil, value)
  }

  def ask(value: Any): CompletableFuture[ActorMessage] = {
    val f = new CompletableFuture[ActorMessage]
    accept(AskMessage((msg: ActorMessage) => {
      f.complete(msg)
    }, value, f))
    f
  }

  def accept(r: ActorRef, m: Any): Unit = {
    accept(ActorMessage(r, m))
  }

}
