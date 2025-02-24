package org.wisp.stream.iterator

import org.wisp.exceptions.ExceptionHandler
import org.wisp.stream.iterator.message.{End, HasNext, Next}
import org.wisp.{ActorLink, Message}

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable

class MessageRouter(handler:ExceptionHandler, prev:Iterable[ActorLink]) extends ActorLink{
  def this(handler:ExceptionHandler, l:ActorLink*) = this(handler, l)

  private val lock = new ReentrantLock()

  private val nodes: util.Queue[ActorLink] = createNodes()

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
  }

  protected class State(val link:ActorLink) extends Comparable[State] {
    var requested = 0
    var ended = false

    override def compareTo(o: State): Int = {
      requested.compareTo(o.requested)
    }
  }

  protected def createState(link:ActorLink): State = {
    State(link)
  }

  private val state:Map[ActorLink, State] = {
    val m = mutable.Map[ActorLink, State]()
    for(p <- prev) m(p) = createState(p)
    m.toMap
  }

  protected def select(s:Map[ActorLink, State]): State = {
    s.values.filter(i => !i.ended).min
  }

  override def accept(t: Message): Unit = {
    lock.lock()
    try {
      t.message match {
        case HasNext =>
          if(state.values.forall(_.ended)){
            t.sender << End
          }else{
            nodes.add(t.sender)
            val n = select(state)
            n.requested += 1
            n.link.ask(HasNext).whenComplete(handler >> this)
          }

        case Next(v) =>
          val st = state(t.sender)
          if(st.ended) throw new IllegalStateException("ended")

          st.requested -= 1
          if(st.requested < 0) throw new IllegalStateException("unexpected Next "+st.requested)

          val n = nodes.poll()
          if (n == null) throw new IllegalStateException("no workers found for " + v)

          n << Next(v)

        case End =>
          val st = state(t.sender)
          if (st.ended) throw new IllegalStateException("ended")
          st.ended = true

          st.requested -= 1
          if (st.requested != 0) throw new IllegalStateException("missing " + st.requested + " messages")

          if (state.values.forall(_.ended)) {
            var a = nodes.poll()
            while (a != null) {
              a << End
              a = nodes.poll()
            }
          } else {
            val n = select(state)
            n.requested += 1
            n.link.ask(HasNext).whenComplete(handler >> this)
          }
      }
    } finally {
      lock.unlock()
    }
  }

}
