package org.wisp.stream.iterator

import org.wisp.{Actor, ActorContext, ActorRef}
import java.util

object MapFlow{

  def apply(prev: ActorRef, context: ActorContext)(fn: Any => Any): MapFlow = {
    new MapFlow(prev, context, new util.LinkedList[ActorRef]())(fn)
  }

  def apply(prev:ActorRef, context: ActorContext, nodes:util.Queue[ActorRef])(fn: Any => Any) : MapFlow = {
    new MapFlow(prev, context, nodes)(fn)
  }

}

class MapFlow(prev:ActorRef, context: ActorContext, nodes:util.Queue[ActorRef])(fn: Any => Any) extends Actor(context){

  private var ended = false

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case Next(v) =>
      if(ended) throw new IllegalStateException("ended")

      val n = nodes.poll()
      if(n == null) throw new IllegalStateException("no workers found for "+v)

      val r = fn.apply(v)
      n << Next(r)

    case HasNext =>
      if(ended) {
        sender << End
      }else{
        nodes.add(sender)
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
