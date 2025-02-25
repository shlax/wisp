package org.wisp

import org.wisp.exceptions.UndeliveredException

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.annotation.targetName

@FunctionalInterface
trait ActorLink extends Consumer[Message]{

  @targetName("send")
  def <<(v:Any) : Unit = {
    val msg = Message( t => { throw UndeliveredException(t) }, v)
    accept(msg)
  }

  def ask(v:Any) : CompletableFuture[Message] = {
    val cf = CompletableFuture[Message]()
    val msg = Message( t => { if (!cf.complete(t)) throw UndeliveredException(t) }, v)
    accept(msg)
    cf
  }

}
