package org.qwActor.stream.iterator

import org.qwActor.stream.iterator.messages.{HasNext, Next, End}
import org.qwActor.{ActorMessage, ActorRef}

import java.util.concurrent.locks.ReentrantLock
import java.util.function
import java.util

object FlatMapStream{

  def apply(prev:ActorRef)(fn: function.Function[Any, Source[_]]): FlatMapStream = {
    new FlatMapStream(prev, new util.LinkedList[ActorRef]())(fn)
  }

}

class FlatMapStream(prev:ActorRef, nodes:util.Queue[ActorRef])(fn: function.Function[Any, Source[_]]) extends ActorRef{
  private val lock = new ReentrantLock()

  private var requested = false
  private var ended = false

  private var current:Option[Source[_]] = None

  override def accept(t: ActorMessage): Unit = {
    lock.lock()
    try{
      t.value match {
        case HasNext =>
          if(ended){
            t.sender << End
          }else{
            current match {
              case Some(s) =>
                val e = s.next()
                e match{
                  case Some(v) =>
                    t.sender << Next(v)
                  case None =>
                    nodes.add(t.sender)
                    current = None
                    requested = true
                    prev.ask(HasNext).thenAccept(this)
                }
              case None =>
                nodes.add(t.sender)
                if(!requested){
                  requested = true
                  prev.ask(HasNext).thenAccept(this)
                }
            }
          }
        case Next(e) =>
          if(current.isDefined) throw new IllegalStateException("current has value "+current)
          if(!requested) throw new IllegalStateException("not requested")

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
    }finally {
      lock.unlock()
    }
  }

}
