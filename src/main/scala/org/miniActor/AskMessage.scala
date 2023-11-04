package org.miniActor

import java.util.concurrent.CompletableFuture

object AskMessage{
  
  def apply(sender:ActorRef, value:Any, callBack:CompletableFuture[ActorMessage]) = new AskMessage(sender, value, callBack)

  def unapply(m: AskMessage): Some[(ActorRef, Any, CompletableFuture[ActorMessage])] = Some((m.sender, m.value, m.callBack))
}

class AskMessage(sender:ActorRef, value:Any, val callBack:CompletableFuture[ActorMessage]) extends ActorMessage(sender, value){
  override def toString: String = "AskMessage("+sender+", "+value+")"
}

