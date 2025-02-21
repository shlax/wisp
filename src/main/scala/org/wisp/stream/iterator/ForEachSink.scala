package org.wisp.stream.iterator

import org.wisp.{ActorRef, ActorSystem, Message}
import org.wisp.stream.Source
import org.wisp.stream.iterator.message.Next

import java.util
import java.util.concurrent.locks.ReentrantLock
import java.util.function.{BiConsumer, Consumer}

object ForEachSink {

  def apply(it:Source[?], system:ActorSystem, prev: Seq[ActorRef], f: Consumer[Any]): ForEachSink = {
    new ForEachSink(it, system, prev, (_, m) => f.accept(m) )
  }

  def apply(it:Source[?], system:ActorSystem, prev: ActorRef, f: Consumer[Any]): ForEachSink = {
    new ForEachSink(it, system, Seq(prev), (_, m) => f.accept(m) )
  }

  def apply(it:Source[?], system:ActorSystem, prev: Seq[ActorRef], f: BiConsumer[ActorRef, Any]): ForEachSink = {
    new ForEachSink(it, system, prev, f)
  }

  def apply(it:Source[?], system:ActorSystem, prev: ActorRef, f: BiConsumer[ActorRef, Any]): ForEachSink = {
    new ForEachSink(it, system, Seq(prev), f)
  }

}

class ForEachSink(it:Source[?], system:ActorSystem, prev: Seq[ActorRef], fn:BiConsumer[ActorRef, Any]) extends ActorRef(system), Runnable {

  private val nodes: util.Queue[ActorRef] = createNodes()

  protected def createNodes(): util.Queue[ActorRef] = {
    util.LinkedList[ActorRef]()
  }

  private val values: util.Queue[(actor:ActorRef, value:Any)] = createValues()

  protected def createValues(): util.Queue[(actor:ActorRef, value:Any)] = {
    util.LinkedList[(actor:ActorRef, value:Any)]()
  }

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private val ended = Array.fill(prev.size)(false)
  private var inputEnded = false

  private def next(p: ActorRef): Unit = {
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
          values.add((t.sender, v))
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
