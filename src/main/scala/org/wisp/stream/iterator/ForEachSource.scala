package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*

import java.util
import org.wisp.lock.*

import java.util.concurrent.locks.Condition

class ForEachSource[T](it:Source[T]) extends StreamActorLink, ActorLink, Runnable {

  protected val nodes:util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected val condition: Condition = lock.newCondition()

  protected var ended = false

  override def run():Unit = lock.withLock {
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
  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if (ended) {
        sender << End
      } else {
        nodes.add(sender)
        condition.signal()
      }
  }

}
