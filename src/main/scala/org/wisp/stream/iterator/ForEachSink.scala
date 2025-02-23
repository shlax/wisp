package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.stream.Source
import org.wisp.stream.iterator.message.{End, HasNext, Next}

import java.util
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import java.util.function

class ForEachSink(it:Source[?], fn:Consumer[Any])(pf: ActorLink => ActorLink) extends ActorLink, Runnable {

  private val nodes: util.Queue[ActorLink] = createNodes()

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
  }

  protected class ActorValue(val actor:ActorLink, val value:Any)
  private val values: util.Queue[ActorValue] = createValues()

  protected def createValues(): util.Queue[ActorValue] = {
    util.LinkedList[ActorValue]()
  }

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private val prev = pf.apply(this)

  private var ended = false
  private var inputEnded = false

  private def next(p:ActorLink): Unit = {
    p.ask(HasNext).thenAccept(this)
  }

  override def run(): Unit = {
    lock.lock()
    try {
      next(prev)

      while (!ended){
        var v = values.poll()
        while(v != null){
          fn.accept(v.value)
          next(v.actor)
          v = values.poll()
        }

        var a = nodes.poll()
        while (a != null){
          if(inputEnded){
            a << End
          }else {
            it.next() match {
              case Some(v) =>
                a << Next(v)
              case None =>
                inputEnded = true
                a << End
            }
          }
          a = nodes.poll()
        }

        if(!ended){
          condition.await()
        }
      }
    } finally {
      lock.unlock()
    }
  }

  override def accept(t: Message): Unit = {
    lock.lock()
    try {
      t.message match {
        case HasNext =>
          if (inputEnded) {
            t.sender << End
          } else {
            nodes.add(t.sender)
            condition.signal()
          }
        case Next(v) =>
          if(ended) throw new IllegalStateException("ended")
          values.add(ActorValue(t.sender, v))
          condition.signal()
        case End =>
          if(ended) throw new IllegalStateException("ended")
          ended = true
          condition.signal()
      }
    } finally {
      lock.unlock()
    }
  }

}
