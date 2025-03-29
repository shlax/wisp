package org.wisp.stream.iterator

import org.wisp.lock.*
import org.wisp.stream.Sink
import org.wisp.stream.iterator.message.Operation
import org.wisp.{ActorLink, Message}

import java.util.concurrent.locks.ReentrantLock
import org.wisp.Consumer

abstract class StreamActorLink extends StreamConsumer{

  protected val lock = new ReentrantLock()

  /** method is running with lock */
  def accept(from:ActorLink): PartialFunction[Operation, Unit]

  override def accept(t: Message): Unit = lock.withLock{
    val f = accept(t.sender)
    f.apply(t.value.asInstanceOf[Operation])
  }

  protected def complete(c: Sink[?], tr: Option[Throwable]): Unit = {
    if(tr.isDefined) throw tr.get
    c.complete()
  }

}
