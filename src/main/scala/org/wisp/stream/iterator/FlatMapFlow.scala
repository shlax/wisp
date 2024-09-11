package org.wisp.stream.iterator

import org.wisp.{Actor, ActorContext, ActorRef}
import java.util

object FlatMapFlow{

  def apply(prev:ActorRef, context: ActorContext)(fn: Any => Source[?]): FlatMapFlow = {
    new FlatMapFlow(prev, context, new util.LinkedList[ActorRef]())(fn)
  }

  def apply(prev:ActorRef, context: ActorContext, nodes:util.Queue[ActorRef])(fn: Any => Source[?]): FlatMapFlow = {
    new FlatMapFlow(prev, context, nodes)(fn)
  }

}

class FlatMapFlow(prev:ActorRef, context: ActorContext, nodes:util.Queue[ActorRef])(fn: Any => Source[?]) extends Actor(context){

  private var current:Option[Source[?]] = None
  private var requested = false
  private var ended = false

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case HasNext =>
      if(ended){
        sender << End
      }else{
        current match {
          case Some(s) =>
            val e = s.next()
            e match{
              case Some(v) =>
                sender << Next(v)
              case None =>
                nodes.add(sender)
                current = None
                requested = true
                prev.ask(HasNext).thenAccept(this)
            }
          case None =>
            nodes.add(sender)
            if(!requested){
              requested = true
              prev.ask(HasNext).thenAccept(this)
            }
        }
      }
    case Next(e) =>
      if(current.isDefined) throw new IllegalStateException("current has value "+current)
      if(!requested) throw new IllegalStateException("not requested")
      if(ended) throw new IllegalStateException("ended")

      requested = false

      val s = fn.apply(e)
      current = Some(s)

      while (!nodes.isEmpty && current.isDefined) {
        val e = s.next()
        e match{
          case Some(v) =>
            val n = nodes.poll()
            n << Next(v)
          case None =>
            current = None
            if(!nodes.isEmpty) {
              requested = true
              prev.ask(HasNext).thenAccept(this)
            }
        }
      }

    case End =>
      if (current.isDefined) throw new IllegalStateException("current has value " + current)
      if (!requested) throw new IllegalStateException("not requested")

      requested = false
      ended = true

      var a = nodes.poll()
      while (a != null) {
        a << End
        a = nodes.poll()
      }

  }

}
