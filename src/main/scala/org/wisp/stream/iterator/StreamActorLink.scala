package org.wisp.stream.iterator

import org.wisp.utils.lock.*
import org.wisp.{ActorLink, Message}
import java.util.concurrent.locks.ReentrantLock

trait StreamActorLink extends ActorLink {

  protected def lock: ReentrantLock

  /**
   * method is running with lock
   */
  def apply(from:ActorLink): PartialFunction[Operation, Unit]

  override def apply(t: Message): Unit = lock.withLock{
    t.process(StreamActorLink.this.getClass) {
      val f = apply(t.sender)
      f.apply(t.value.asInstanceOf[Operation])
    }
  }
  
}
