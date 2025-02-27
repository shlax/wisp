package org.wisp

import org.wisp.exceptions.UndeliveredException

import scala.annotation.targetName
import scala.concurrent.Promise

@FunctionalInterface
trait ActorLink extends Consumer[Message]{

  @targetName("send")
  def <<(v:Any) : Unit = {
    val msg = Message( t => { throw UndeliveredException(t) }, v)
    accept(msg)
  }

  def ask(v:Any) : Promise[Message] = {
    val cf = Promise[Message]()
    val msg = Message( t => { if (!cf.trySuccess(t)) throw UndeliveredException(t) }, v)
    accept(msg)
    cf
  }

}
