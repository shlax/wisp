package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.*

import java.util.concurrent.locks.ReentrantLock

class ActorSource(src:Source[?]) extends ActorLink{

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
