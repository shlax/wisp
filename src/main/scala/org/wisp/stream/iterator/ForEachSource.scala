package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.{ActorLink, ActorSystem, Message}
import org.wisp.stream.iterator.message.*

import java.util
import java.util.concurrent.locks.ReentrantLock

class ForEachSource(it:Source[?], system:ActorSystem) extends ActorLink(system), Runnable {

  private val nodes:util.Queue[ActorLink] = createNodes()

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
  }

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private var ended = false

  override def run():Unit = {
    lock.lock()
    try {
      it.forEach { e =>
        var a = nodes.poll()
        while (a == null){
          condition.await()
          a = nodes.poll()
        }
        a << Next(e)
      }

      ended = true
      var a = nodes.poll()
      while (a != null) {
        a << End
        a = nodes.poll()
      }
    } finally {
      lock.unlock()
    }
  }

  override def accept(t: Message): Unit = {
    lock.lock()
    try {
      t.message match {
        case HasNext =>
          if (ended) {
            t.sender << End
          } else {
            nodes.add(t.sender)
            condition.signal()
          }
      }
    } finally {
      lock.unlock()
    }
  }

}
