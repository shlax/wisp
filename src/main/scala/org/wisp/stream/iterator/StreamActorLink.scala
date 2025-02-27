package org.wisp.stream.iterator

import org.wisp.lock.*
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.message.IteratorMessage
import org.wisp.{ActorLink, Message}

import java.io.Flushable
import java.util.concurrent.locks.ReentrantLock
import org.wisp.Consumer
import scala.util.control.NonFatal

abstract class StreamActorLink extends Consumer[Message], StreamException{

  protected val lock = new ReentrantLock()

  /** method is running with lock */
  def accept(from:ActorLink): PartialFunction[IteratorMessage, Unit]

  override def accept(t: Message): Unit = {
    val f = accept(t.sender)
    lock.withLock{ f.apply(t.value.asInstanceOf[IteratorMessage]) }
  }

  protected def flush(c: Flushable, tr: Option[Throwable]): Unit = {
    if(tr.isDefined){
      throw tr.get
    }

    try {
      c.flush()
    } catch {
      case NonFatal(ex) =>
        throw ex
    }

  }

}
