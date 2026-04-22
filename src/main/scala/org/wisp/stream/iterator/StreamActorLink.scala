package org.wisp.stream.iterator

import org.wisp.utils.lock.*
import org.wisp.{ActorLink, Message}

trait StreamActorLink[T] extends ActorLink[Operation[T]], StreamLock {

  /**
   * method is running with lock
   */
  def apply(from:ActorLink[Operation[T]]): PartialFunction[Operation[T], Unit]

  override def apply(t: Message[Operation[T]]): Unit = lock.withLock{
    t.process(StreamActorLink.this.getClass) {
      val f = apply(t.sender.asInstanceOf[ActorLink[Operation[T]]])
      f.apply(t.value)
    }
  }
  
}
