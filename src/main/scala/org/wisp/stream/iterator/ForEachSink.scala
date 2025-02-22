package org.wisp.stream.iterator

import org.wisp.{ActorLink, ActorSystem, Message}
import org.wisp.stream.Source
import org.wisp.stream.iterator.message.{End, HasNext, Next}

import java.util
import java.util.concurrent.locks.ReentrantLock
import java.util.function.{BiConsumer, Consumer}
import java.util.function
import scala.annotation.targetName

object ForEachSink {

  @targetName("applySeq")
  def apply(it:Source[?], system:ActorSystem, f: Consumer[Any])(prev: ActorLink => Seq[ActorLink]): ForEachSink = {
    new ForEachSink(it, system, (_, m) => f.accept(m))(prev)
  }

  @targetName("applyOne")
  def apply(it:Source[?], system:ActorSystem, f: Consumer[Any])(prev: ActorLink => ActorLink): ForEachSink = {
    new ForEachSink(it, system, (_, m) => f.accept(m))(r => Seq(prev.apply(r)) )
  }

  @targetName("applySeq")
  def apply(it:Source[?], system:ActorSystem, f: BiConsumer[ActorLink, Any])(prev: ActorLink => Seq[ActorLink]): ForEachSink = {
    new ForEachSink(it, system, f)(prev)
  }

  @targetName("applyOne")
  def apply(it:Source[?], system:ActorSystem,  f: BiConsumer[ActorLink, Any])(prev: ActorLink => ActorLink): ForEachSink = {
    new ForEachSink(it, system, f)(r => Seq(prev.apply(r)) )
  }

}

class ForEachSink(it:Source[?], system:ActorSystem, fn:BiConsumer[ActorLink, Any])(pf: ActorLink => Seq[ActorLink]) extends ActorLink(system), Runnable {

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

  private val ended = Array.fill(prev.size)(false)
  private var inputEnded = false

  private def next(p: ActorLink): Unit = {
    p.ask(HasNext).thenAccept(this)
  }

  override def run(): Unit = {
    lock.lock()
    try {
      for(p <- prev) next(p)

      while (ended.contains(false)){
        var v = values.poll()
        while(v != null){
          fn.accept(v.actor, v.value)
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

        if(ended.contains(false)){
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
          val ind = prev.indexOf(t.sender)
          if(ended(ind)) throw new IllegalStateException("ended")
          values.add(ActorValue(t.sender, v))
          condition.signal()
        case End =>
          val ind = prev.indexOf(t.sender)
          if(ended(ind)) throw new IllegalStateException("ended")
          ended(ind) = true
          condition.signal()
      }
    } finally {
      lock.unlock()
    }
  }

}
