package org.wisp.stream.iterator

import org.wisp.lock.*
import org.wisp.stream.iterator.message.IteratorMessage
import org.wisp.{ActorLink, Message}

import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

abstract class StreamActorLink extends Consumer[Message]{
  protected val lock = new ReentrantLock()

  def accept(from:ActorLink): PartialFunction[IteratorMessage, Unit]

  override def accept(t: Message): Unit = {
    val f = accept(t.sender)
    lock.withLock{ f.apply(t.value.asInstanceOf[IteratorMessage]) }
  }
}
