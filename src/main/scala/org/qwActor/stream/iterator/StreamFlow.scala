package org.qwActor.stream.iterator

import org.qwActor.stream.iterator.messages.{End, HasNext, Next}
import org.qwActor.{Actor, ActorContext, ActorMessage, ActorRef}

import java.util

object StreamFlow{

  def apply(prev: ActorRef, context: ActorContext)(fn: PartialFunction[Any, Any]): StreamFlow = {
    new StreamFlow(prev, context, new util.LinkedList[ActorRef]())(fn)
  }

  def apply(prev:ActorRef, context: ActorContext, nodes:util.Queue[ActorRef])(fn: PartialFunction[Any, Any]) : StreamFlow = {
    new StreamFlow(prev, context, nodes)(fn)
  }

}

class StreamFlow(prev:ActorRef, context: ActorContext, nodes:util.Queue[ActorRef])(fn: PartialFunction[Any, Any]) extends Actor(context){

  private var ended = false

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case Next(v) =>
      val n = nodes.poll()
      if(n == null){
        throw new IllegalStateException("no workers found for "+v)
      }

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
