package org.wisp.stream.iterator

import org.wisp.lock.*
import org.wisp.stream.Sink
import org.wisp.stream.iterator.message.IteratorMessage
import org.wisp.{ActorLink, Message}

import java.util.concurrent.locks.ReentrantLock
import org.wisp.Consumer
import scala.util.control.NonFatal

abstract class StreamActorLink extends Consumer[Message], StreamException{

  protected val lock = new ReentrantLock()

  /** method is running with lock */
  def accept(from:ActorLink): PartialFunction[IteratorMessage, Unit]

  override def accept(t: Message): Unit = lock.withLock{
    val f = accept(t.sender)
    f.apply(t.value.asInstanceOf[IteratorMessage])
  }

  protected def flush(c: Sink[?], tr: Option[Throwable]): Unit = {
    if(tr.isDefined) throw tr.get
    c.flush()
  }

}
