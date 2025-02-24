package org.wisp.stream.iterator

import org.wisp.stream.iterator.message.{End, HasNext, Next}
import org.wisp.{ActorLink, Message}

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable

class MessageRouter(prev:Iterable[ActorLink]) extends ActorLink{
  private val lock = new ReentrantLock()

  private val nodes: util.Queue[ActorLink] = createNodes()

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
  }

  protected class State extends Comparable[State] {
    var requested = 0
    var ended = false

    override def compareTo(o: State): Int = {
      requested.compareTo(o.requested)
    }
  }

  protected def createState(): State = {
    State()
  }

  private val state:Map[ActorLink, State] = {
    val m = mutable.Map[ActorLink, State]()
    for(p <- prev) m(p) = createState()
    m.toMap
  }

  protected def select(s:Map[ActorLink, State]): (ActorLink, State) = {
    s.minBy(_._2)
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
            n._2.requested += 1
            n._1 << HasNext
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
          if(st.ended) throw new IllegalStateException("ended")
          st.ended = true

          st.requested -= 1
          if(st.requested != 0) throw new IllegalStateException("missing "+st.requested+" messages")

          if(state.values.forall(_.ended)){
            var a = nodes.poll()
            while (a != null) {
              a << End
              a = nodes.poll()
            }
          }
      }
    } finally {
      lock.unlock()
    }
  }

}
