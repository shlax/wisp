package org.wisp.stream.typed

import org.wisp.ActorSystem
import org.wisp.stream.Source
import org.wisp.stream.iterator.ActorSource

class Stream(val system:ActorSystem){

  def from[T](s:Source[T]) : Node[T] = {
    new Node[T](this, ActorSource(s))
  }

}
