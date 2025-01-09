package org.wisp

object Message {

  def unapply(m:Message) : (ActorRef, Any) = (m.sender, m.message)
  
}

class Message(val sender:ActorRef, val message:Any)
