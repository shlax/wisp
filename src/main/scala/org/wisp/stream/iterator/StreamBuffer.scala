package org.wisp.stream.iterator

import org.wisp.exceptions.ExceptionHandler
import org.wisp.lock.*
import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.*

import java.util
import java.util.concurrent.locks.ReentrantLock

class StreamBuffer(eh: ExceptionHandler, prev:ActorLink, size:Int) extends ActorLink{
  private val lock = new ReentrantLock()

  private var ended = false
  private var requested = false

  private val queue:util.Queue[Any] = createQueue()

  protected def createQueue(): util.Queue[Any] = {
    util.LinkedList[Any]()
  }

  private val nodes: util.Queue[ActorLink] = createNodes()

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
  }

  private def next(): Unit = {
    val req = if(requested) 1 else 0
    if (!ended && queue.size() + req < size) {
      requested = true
      prev.ask(HasNext).whenComplete(eh >> this)
    }
  }

  override def accept(t: Message): Unit = lock.withLock{
    t.value match {

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
        if(ended) throw new IllegalStateException("ended")
        requested = false

        val n = nodes.poll()
        if (n == null) {
          queue.add(v)
        } else {
          n << Next(v)
        }

        next()

      case End =>
        if(ended) throw new IllegalStateException("ended")
        requested = false
        ended = true

        if (queue.isEmpty) {
          var a = nodes.poll()
          while (a != null) {
            a << End
            a = nodes.poll()
          }
        }
    }
  }

}
