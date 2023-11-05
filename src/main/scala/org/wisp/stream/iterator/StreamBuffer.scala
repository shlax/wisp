package org.wisp.stream.iterator

import org.wisp.{ActorMessage, ActorRef}

import java.util
import java.util.concurrent.locks.ReentrantLock

object StreamBuffer{

  def apply(prev: ActorRef, nodes:util.Queue[ActorRef], queue: util.Queue[Any], size:Int): StreamBuffer = {
    new StreamBuffer(prev, nodes, queue, size)
  }

  def apply(prev: ActorRef, size: Int): StreamBuffer = {
    new StreamBuffer(prev, new util.LinkedList[ActorRef](), new util.LinkedList[Any](), size)
  }

}

class StreamBuffer(prev:ActorRef, nodes:util.Queue[ActorRef], queue: util.Queue[Any], size:Int) extends ActorRef{
  private val lock = new ReentrantLock()

  private var ended = false
  private var requested = 0

  private def next(): Unit = {
    if (!ended && queue.size() + requested < size) {
      requested += 1
      prev.ask(HasNext).thenAccept(this)
    }
  }


  override def accept(t: ActorMessage): Unit = {
    lock.lock()
    try {
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
    }finally {
      lock.unlock()
    }
  }

}
