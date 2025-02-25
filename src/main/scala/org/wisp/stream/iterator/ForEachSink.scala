package org.wisp.stream.iterator

import org.wisp.exceptions.ExceptionHandler
import org.wisp.ActorLink
import org.wisp.stream.Source
import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}

import java.util
import java.util.function.Consumer
import java.util.function
import org.wisp.lock.*

import java.util.concurrent.locks.Condition

class ForEachSink[F, T](eh: ExceptionHandler, src:Source[F], sink:Consumer[T])(link: ActorLink => ActorLink) extends StreamActorLink, ActorLink, Runnable {

  protected val nodes: util.Queue[ActorLink] = createNodes()

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
  }

  protected class ActorValue(val actor:ActorLink, val value:Any)
  protected val values: util.Queue[ActorValue] = createValues()

  protected def createValues(): util.Queue[ActorValue] = {
    util.LinkedList[ActorValue]()
  }

  protected val condition: Condition = lock.newCondition()

  protected val prev:ActorLink = link.apply(this)

  protected var inputEnded = false
  protected var ended = false

  protected def next(p:ActorLink): Unit = {
    p.ask(HasNext).whenComplete(eh >> this)
  }

  override def run(): Unit = lock.withLock {
    next(prev)

    while (!ended) {
      var v = values.poll()
      while (v != null) {
        sink.accept(v.value.asInstanceOf[T])
        next(v.actor)
        v = values.poll()
      }

      var a = nodes.poll()
      while (a != null) {
        if (inputEnded) {
          a << End
        } else {
          src.next() match {
            case Some(v) =>
              a << Next(v)
            case None =>
              inputEnded = true
              a << End
          }
        }
        a = nodes.poll()
      }

      if (!ended) {
        condition.await()
      }
    }
  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if (inputEnded) {
        sender << End
      } else {
        nodes.add(sender)
        condition.signal()
      }
    case Next(v) =>
      if(ended) throw new IllegalStateException("ended")
      values.add(ActorValue(sender, v))
      condition.signal()
    case End =>
      if(ended) throw new IllegalStateException("ended")
      ended = true
      condition.signal()
  }

}
