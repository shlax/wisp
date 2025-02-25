package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.*
import org.wisp.lock.*

class StreamSource[T](src:Source[T]) extends StreamActorLink, ActorLink{

  protected var ended = false

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if (ended) {
        sender << End
      } else {
        val n = src.next()
        if (n.isDefined) {
          sender << Next(n.get)
        } else {
          ended = true
          sender << End
        }
      }
  }

}
