package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*

import java.util
import org.wisp.lock.*

import java.util.concurrent.locks.Condition
import scala.util.control.NonFatal

class ForEachSource[T](src:Source[T]) extends StreamActorLink, ActorLink, Runnable {

  protected val nodes:util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected val condition: Condition = lock.newCondition()

  protected var ended = false

  override def run():Unit = lock.withLock {
    var e: Option[Throwable] = None
    try {
      src.forEach { e =>
        var a = nodes.poll()
        while (a == null) {
          condition.await()
          a = nodes.poll()
        }
        a << Next(e)
      }
    } catch {
      case NonFatal(ex) =>
        e = Some(ex)
    }

    ended = true
    var a = nodes.poll()
    while (a != null) {
      a << End(e)
      a = nodes.poll()
    }

    if (e.isDefined) {
      throw e.get
    }
  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if (ended) {
        sender << End()
      } else {
        nodes.add(sender)
        condition.signal()
      }
  }

}
