package org.wisp.stream.iterator

import org.wisp.Message
import org.wisp.stream.iterator.message.End

import java.util.function.{BiConsumer, Consumer}

trait StreamException extends Consumer[Message], BiConsumer[Message, Throwable]{

  override def accept(t: Message, u: Throwable): Unit = {
    if (u != null) {
      accept(Message(t.sender, End(Some(u))))
    } else {
      accept(t)
    }
  }

}
