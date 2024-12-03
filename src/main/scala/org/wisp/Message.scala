package org.wisp

object Message {

  def unapply(m:Message) : (ActorRef, Any) = (m.from, m.message)
  
}

class Message(val from:ActorRef, val message:Any)
