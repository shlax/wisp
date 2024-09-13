package org.wisp.stream.iterator

import org.wisp.bus.EventBus
import org.wisp.{ActorMessage, ActorRef}

import java.util
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

object WaitSourceSink {

  def apply[A](bus:EventBus, it:Source[A])(fn:Consumer[Any]):WaitSourceSink[A] = {
    new WaitSourceSink(bus, it, new util.LinkedList[ActorRef]())(fn)
  }

  def apply[A](bus:EventBus, it:Source[A], nodes:util.Queue[ActorRef])(fn:Consumer[Any]):WaitSourceSink[A] = {
    new WaitSourceSink(bus, it, nodes)(fn)
  }

}

class WaitSourceSink[A](bus:EventBus, it:Source[A], nodes:util.Queue[ActorRef])(fn:Consumer[Any]) extends ActorRef(bus) {

  private val lock = new ReentrantLock
  private val condition = lock.newCondition()

  private var ended = false

  private var value:Option[Any] = None
  private var sourceEnd = false

  def run(prev:ActorRef): Unit = {

    lock.lock()
    try {
      prev.ask(HasNext).thenAccept(this ) // get first

      var elem:Option[A] = it.next()
      if(elem.isEmpty) ended = true

      while ((!ended) || (!sourceEnd)) {

        // process source
        var a = nodes.poll()
        while(a != null){
          if(elem.isEmpty){
            a << End
          }else{
            a << Next(elem.get)

            elem = it.next()
            if(elem.isEmpty) ended = true
          }

          a = nodes.poll()
        }

        // process sink
        if (value.isDefined) {
          val v = value.get
          fn.accept(v)

          value = None
          prev.ask(HasNext).thenAccept(this )
        }

        if((!ended) || (!sourceEnd)){
          condition.await()
        }

      }

    } finally {
      lock.unlock()
    }
  }

  override def accept(t: ActorMessage): Unit = {
    lock.lock()
    try {
      t.value match {
        // process source
        case HasNext =>
          if (ended) {
            t.sender << End
          } else {
            nodes.add(t.sender)
          }

        // process sink
        case Next(v) =>
          if(sourceEnd) throw new IllegalStateException("sourceEnd")
          if(value.isDefined) throw new IllegalStateException("value is defined")

          value = Some(v)
        case End =>
          sourceEnd = true
      }
      condition.signalAll()
    } finally {
      lock.unlock()
    }
  }

}