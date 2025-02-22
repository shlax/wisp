package org.wisp

import org.wisp.exceptions.{ExceptionHandler, UndeliveredException}

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.annotation.targetName

abstract class ActorRef(val exceptionHandler: ExceptionHandler) extends Consumer[Message]{

  @targetName("send")
  def <<(v:Any) : Unit = {
    accept( Message( new ActorRef(exceptionHandler) {
        override def accept(t: Message): Unit = {
          exceptionHandler.handle(UndeliveredException(t))
        }
      },v) )
  }

  // @targetName("ask")
  def ask(v:Any) : CompletableFuture[Message] = {
    val cf = CompletableFuture[Message]()
    accept( Message( new ActorRef(exceptionHandler) {
        override def accept(t: Message): Unit = cf.complete(t)
      },v) )
    cf
  }

}
