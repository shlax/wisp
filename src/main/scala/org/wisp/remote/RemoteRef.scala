package org.wisp.remote

import org.wisp.{ActorMessage, ActorRef}

import scala.annotation.targetName

abstract class RemoteRef(ref:ActorRef) extends ActorRef(ref.eventBus){
  override def accept(msg: ActorMessage): Unit = ref.accept(msg)

  @targetName("send")
  override def << (value: Any): Unit = { ref << value }

  def bind(path:Any) : this.type 
}
