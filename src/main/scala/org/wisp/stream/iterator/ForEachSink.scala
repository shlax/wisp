package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.stream.Source
import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}

import java.util
import java.util.function.{BiConsumer, Consumer}
import java.util.function
import org.wisp.lock.*

import java.util.concurrent.locks.Condition

class ForEachSink[F, T](src:Source[F], sink:Consumer[T])(link: ActorLink => ActorLink)
  extends StreamActorLink, ActorLink, BiConsumer[Message, Throwable], Runnable {

  protected val nodes: util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected val condition: Condition = lock.newCondition()
  protected val prev: ActorLink = link.apply(this)

  protected var value: Option[T] = None
  protected var exception: Option[Throwable] = None

  protected var inputEnded = false
  protected var ended = false

  protected def next(): Unit = {
    prev.ask(HasNext).whenComplete(this)
  }

  override def run(): Unit = lock.withLock {
    autoClose(sink) {
      next()

      while (!ended && exception.isEmpty) {

        for (v <- value) {
          value = None
          sink.accept(v)
          next()
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

        if (!ended && exception.isEmpty) {
          condition.await()
        }
      }

      for (e <- exception) {
        throw e
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
      if(value.isDefined) throw new IllegalStateException("dropped value: "+v)

      value = Some(v.asInstanceOf[T])
      condition.signal()
    case End =>
      if(ended) throw new IllegalStateException("ended")
      ended = true
      condition.signal()
  }

  override def accept(t: Message, u: Throwable): Unit = {
    if(u != null){
      lock.withLock{
        exception = Some(u)
        condition.signal()
      }
    }else{
      accept(t)
    }
  }

}
