package org.qwActor.stream.iterator

import org.qwActor.stream.iterator.messages.{Next, End, HasNext}
import org.qwActor.{Actor, ActorContext, ActorRef}

import java.util

object StreamBuffer{

  def apply(prev: ActorRef, context: ActorContext, nodes:util.Queue[ActorRef], queue: util.Queue[Any], size:Int): StreamBuffer = {
    new StreamBuffer(prev, context, nodes, queue, size)
  }

  def apply(prev: ActorRef, context: ActorContext, size: Int): StreamBuffer = {
    new StreamBuffer(prev, context, new util.LinkedList[ActorRef](), new util.LinkedList[Any](), size)
  }

}

class StreamBuffer(prev:ActorRef, context: ActorContext, nodes:util.Queue[ActorRef], queue: util.Queue[Any], size:Int) extends Actor(context){

  private var ended = false
  private var requested = 0

  private def next(): Unit = {
    if (!ended && queue.size() + requested < size) {
      requested += 1
      prev.ask(HasNext).thenAccept(this)
    }
  }

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {

    case HasNext =>
      val e = queue.poll()
      if(e == null){
        if(ended){
          sender << End
        }else {
          nodes.add(sender)
          next()
        }
      }else{
        sender << Next(e)
        next()
      }

    case Next(v) =>
      requested -= 1

      val n = nodes.poll()
      if(n == null){
        queue.add(v)
      }else{
        n << Next(v)
      }

      next()

    case End =>
      requested -= 1
      ended = true

      if(queue.isEmpty){
        var a = nodes.poll()
        while (a != null) {
          a << End
          a = nodes.poll()
        }
      }

  }

}
