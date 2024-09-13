package org.wisp.stream.iterator

import org.wisp.bus.EventBus
import org.wisp.{ActorMessage, ActorRef}

import java.util.concurrent.locks.ReentrantLock

object StreamSource{

  def apply(bus:EventBus, it:Source[?]):StreamSource = new StreamSource(bus, it)

}

/** Iterator will be called from multiple threads */
class StreamSource(bus:EventBus, it:Source[?]) extends ActorRef(bus) {

  private val lock = new ReentrantLock

  override def accept(t: ActorMessage): Unit = {
    t.value match {
      case HasNext =>
        val v : StreamResponseMessage = {
          lock.lock()
          try {
            it.next() match {
              case Some(i) => Next(i)
              case None => End
            }
          }finally {
            lock.unlock()
          }
        }
        t.sender << v
    }
  }

}
