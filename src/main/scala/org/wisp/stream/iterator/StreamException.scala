package org.wisp.stream.iterator

import org.wisp.exceptions.UndeliveredException
import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.End

import java.util.function.{BiConsumer, Consumer}

trait StreamException extends Consumer[Message], BiConsumer[Message, Throwable]{

  override def accept(t: Message, u: Throwable): Unit = {
    if (u != null) {
      val end = End(Some(u))
      accept(Message( x => { throw UndeliveredException(x) }, end))
    } else {
      accept(t)
    }
  }

}
