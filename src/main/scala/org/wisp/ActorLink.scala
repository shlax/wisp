package org.wisp

import org.wisp.exceptions.UndeliveredException

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.annotation.targetName

@FunctionalInterface
trait ActorLink extends Consumer[Message]{

  @targetName("send")
  def <<(v:Any) : Unit = {
    accept( Message((t: Message) => {
        throw UndeliveredException(t)
      },v) )
  }

  def ask(v:Any) : CompletableFuture[Message] = {
    val cf = CompletableFuture[Message]()
    accept( Message((t: Message) => {
        if(!cf.complete(t)) throw UndeliveredException(t)
      },v) )
    cf
  }

}
