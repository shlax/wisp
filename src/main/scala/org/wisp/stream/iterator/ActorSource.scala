package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.{ActorRef, ActorSystem, Message}

import java.util.concurrent.locks.ReentrantLock

class ActorSource(src:Source[?], system:ActorSystem) extends ActorRef(system){

  private val lock = new ReentrantLock()
  private var ended = false

  override def accept(t: Message): Unit = {
    lock.lock()
    try {
      t.message match {
        case HasNext =>
          if (ended) {
            t.sender << End
          } else {
            val n = src.next()
            if (n.isDefined) {
              t.sender << Next(n.get)
            } else {
              ended = true
              t.sender << End
            }
          }
      }
    }finally {
      lock.unlock()
    }
  }

}
