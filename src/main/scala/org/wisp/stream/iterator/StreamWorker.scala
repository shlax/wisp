package org.wisp.stream.iterator

import org.wisp.{Actor, ActorLink, Inbox, Message}
import org.wisp.stream.iterator.message.*

import java.util
import java.util.function.BiConsumer

class StreamWorker[F, T](prev:ActorLink, inbox:Inbox, fn: F => T) extends Actor(inbox), StreamException{

  protected val nodes:util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected var exception: Option[Throwable] = None
  protected var ended = false

  override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
    case Next(v) =>
      if (ended) throw new IllegalStateException("ended")

      val n = nodes.poll()
      if (n == null) throw new IllegalStateException("no workers found for " + v)

      val r = fn.apply(v.asInstanceOf[F])
      n << Next(r)

    case HasNext =>
      if(exception.isDefined){
        from << End(exception)
      }else {
        if (ended) {
          from << End()
        } else {
          nodes.add(from)
          prev.ask(HasNext).whenComplete(this)
        }
      }

    case End(ex) =>
      if(ex.isDefined) exception = ex
      else ended = true

      var a = nodes.poll()
      while (a != null) {
        a << End(exception)
        a = nodes.poll()
      }
  }



}
