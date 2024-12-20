package org.wisp

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.annotation.targetName

abstract class ActorRef(val system:ActorSystem) extends Consumer[Message]{

  @targetName("send")
  def << (v:Any) : Unit = {
    accept( Message( new ActorRef(system) {
        override def accept(t: Message): Unit = system.handle(t)
      },v) )
  }

  @targetName("ask")
  def ?< (v:Any) : CompletableFuture[Message] = {
    val cf = CompletableFuture[Message]()
    accept( Message( new ActorRef(system) {
        override def accept(t: Message): Unit = cf.complete(t)
      },v) )
    cf
  }

}
