package org.wisp.stream.iterator

import org.wisp.{ActorRef, Message}
import org.wisp.stream.iterator.message.*

import java.util
import java.util.concurrent.locks.ReentrantLock

class MapFlow(prev:ActorRef)(fn: Any => Any) extends ActorRef(prev.system){
  private val lock = new ReentrantLock()

  private val nodes:util.Queue[ActorRef] = createNodes()
  private var ended = false

  protected def createNodes(): util.Queue[ActorRef] = {
    util.LinkedList[ActorRef]()
  }

  override def accept(t: Message): Unit = {
    lock.lock()
    try{
      t.message match {
        case Next(v) =>
          if (ended) throw new IllegalStateException("ended")

          val n = nodes.poll()
          if (n == null) throw new IllegalStateException("no workers found for " + v)

          val r = fn.apply(v)
          n << Next(r)

        case HasNext =>
          if (ended) {
            t.sender << End
          } else {
            nodes.add(t.sender)
            prev.ask(HasNext).thenAccept(this)
          }

        case End =>
          ended = true
          var a = nodes.poll()
          while (a != null) {
            a << End
            a = nodes.poll()
          }
      }
    }finally {
      lock.unlock()
    }
  }

}
