package org.miniActor.remote

import org.miniActor.{ActorMessage, ActorRef}

import scala.annotation.targetName

abstract class RemoteRef(ref:ActorRef) extends ActorRef{
  override def accept(msg: ActorMessage): Unit = ref.accept(msg)

  @targetName("send")
  override def << (value: Any): Unit = { ref << value }

  def bind(path:Any) : this.type 
}
