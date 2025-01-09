package org.wisp.stream.iterator

import org.wisp.{ActorRef, Message}
import org.wisp.stream.Source

class FlatMapFlow(prev:ActorRef)(fn: Any => Source[?]) extends ActorRef(prev.system){



  override def accept(t: Message): Unit = {

  }

}
