package org.qwActor.remote

import org.qwActor.{ActorMessage, ActorRef}

import scala.annotation.targetName

abstract class RemoteRef(ref:ActorRef) extends ActorRef{
  override def accept(msg: ActorMessage): Unit = ref.accept(msg)

  @targetName("send")
  override def << (value: Any): Unit = { ref << value }

  def bind(path:Any) : this.type 
}
