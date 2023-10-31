package org.qwActor.stream.iterator

import org.qwActor.{ActorMessage, ActorRef}
import org.qwActor.stream.iterator.messages.{End, HasNext, IteratorMessage, Next}

import java.util
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

object WaitSourceSink {

  def apply[A](it:Source[A])(fn:Consumer[Any]):WaitSourceSink[A] = {
    new WaitSourceSink(it, new util.LinkedList[ActorRef]())(fn)
  }

  def apply[A](it:Source[A], nodes:util.Queue[ActorRef])(fn:Consumer[Any]):WaitSourceSink[A] = {
    new WaitSourceSink(it, nodes)(fn)
  }

}

class WaitSourceSink[A](it:Source[A], nodes:util.Queue[ActorRef])(fn:Consumer[Any]) extends ActorRef {

  private val lock = new ReentrantLock
  private val condition = lock.newCondition()

  private var ended = false

  private var value:Option[Any] = None
  private var end = false

  def run(prev:ActorRef): Unit = {

    try {
      lock.lock()
      prev.ask(HasNext).thenAccept(this ) // get first

      var elem:Option[A] = it.next()
      if(elem.isEmpty) ended = true

      while ((!ended) || (!end)) {

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

        condition.await()

      }

    } finally {
      lock.unlock()
    }
  }

  override def accept(t: ActorMessage): Unit = {
    try {
      lock.lock()
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
          if (value.isDefined) {
            throw new IllegalStateException("value.isDefined")
          }
          value = Some(v)
        case End =>
          end = true
      }
      condition.signalAll()
    } finally {
      lock.unlock()
    }
  }

}