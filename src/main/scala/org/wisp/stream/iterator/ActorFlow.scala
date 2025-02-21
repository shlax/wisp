package org.wisp.stream.iterator

import org.wisp.{Actor, ActorRef, Inbox}
import org.wisp.stream.iterator.message.*

import java.util

class ActorFlow(prev:ActorRef, inbox:Inbox, fn: Any => Any) extends Actor(inbox){

  private val nodes:util.Queue[ActorRef] = createNodes()
  private var ended = false

  protected def createNodes(): util.Queue[ActorRef] = {
    util.LinkedList[ActorRef]()
  }

  override def accept(from: ActorRef): PartialFunction[Any, Unit] = {
    case Next(v) =>
      if (ended) throw new IllegalStateException("ended")

      val n = nodes.poll()
      if (n == null) throw new IllegalStateException("no workers found for " + v)

      val r = fn.apply(v)
      n << Next(r)

    case HasNext =>
      if (ended) {
        from << End
      } else {
        nodes.add(from)
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

}
