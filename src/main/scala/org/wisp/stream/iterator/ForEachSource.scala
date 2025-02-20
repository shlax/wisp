package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.{ActorRef, ActorSystem, Message}
import org.wisp.stream.iterator.message.*

import java.util
import java.util.concurrent.locks.ReentrantLock

class ForEachSource(it:Source[?], system:ActorSystem) extends ActorRef(system), Runnable {

  private val nodes:util.Queue[ActorRef] = createNodes()

  protected def createNodes(): util.Queue[ActorRef] = {
    util.LinkedList[ActorRef]()
  }

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private var ended = false

  override def run():Unit = {
    it.forEach { e =>
      lock.lock()
      try {
        var a = nodes.poll()
        while (a == null){
          condition.await()
          a = nodes.poll()
        }
        a << Next(e)
      } finally {
        lock.unlock()
      }
    }

    lock.lock()
    try {
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
    t.message match {
      case HasNext =>
        lock.lock()
        try {
          if (ended) {
            t.sender << End
          } else {
            nodes.add(t.sender)
            condition.signal()
          }
        } finally {
          lock.unlock()
        }

    }
  }

}
