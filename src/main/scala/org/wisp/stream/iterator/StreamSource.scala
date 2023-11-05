package org.wisp.stream.iterator

import org.wisp.{ActorMessage, ActorRef}
import java.util.concurrent.locks.ReentrantLock

object StreamSource{

  def apply(it:Source[_]):StreamSource = new StreamSource(it)

}

/** Iterator will be called from multiple threads */
class StreamSource(it:Source[_]) extends ActorRef {

  private val lock = new ReentrantLock

  override def accept(t: ActorMessage): Unit = {
    t.value match {
      case HasNext =>
        val v : StreamResponseMessage = try {
          lock.lock()
          it.next() match {
            case Some(i) => Next(i)
            case None => End
          }
        }finally {
          lock.unlock()
        }
        t.sender << v
    }
  }

}
