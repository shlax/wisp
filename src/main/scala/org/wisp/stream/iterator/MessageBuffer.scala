package org.wisp.stream.iterator

import org.wisp.{ActorRef, Message}

import java.util
import java.util.concurrent.locks.ReentrantLock

class MessageBuffer(prev:ActorRef, val size:Int) extends ActorRef(prev.system){
  private val lock = new ReentrantLock()

  private var ended = false
  private var requested = 0

  private val queue:util.Queue[Any] = createQueue()

  protected def createQueue(): util.Queue[Any] = {
    util.LinkedList[Any]()
  }

  private val nodes: util.Queue[ActorRef] = createNodes()

  protected def createNodes(): util.Queue[ActorRef] = {
    util.LinkedList[ActorRef]()
  }

  private def next(): Unit = {
    if (!ended && queue.size() + requested < size) {
      requested += 1
      prev.ask(HasNext).thenAccept(this)
    }
  }

  override def accept(t: Message): Unit = {
    lock.lock()
    try {
      t.message match {

        case HasNext =>
          val e = queue.poll()
          if (e == null) {
            if (ended) {
              t.sender << End
            } else {
              nodes.add(t.sender)
              next()
            }
          } else {
            t.sender << Next(e)
            next()
          }

        case Next(v) =>
          if (ended) throw new IllegalStateException("ended")

          requested -= 1

          val n = nodes.poll()
          if (n == null) {
            queue.add(v)
          } else {
            n << Next(v)
          }

          next()

        case End =>
          requested -= 1
          ended = true

          if (queue.isEmpty) {
            var a = nodes.poll()
            while (a != null) {
              a << End
              a = nodes.poll()
            }
          }
      }
    } finally {
      lock.unlock()
    }
  }

}
