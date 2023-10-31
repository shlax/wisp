package org.qwActor.stream.iterator

import org.qwActor.{ActorMessage, ActorRef}
import org.qwActor.stream.iterator.messages.{End, HasNext, Next}

import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

object WaitSink{

  def apply(prev:ActorRef)(fn:Consumer[Any]):WaitSink = new WaitSink(prev)(fn)

}

class WaitSink(prev:ActorRef)(fn:Consumer[Any]) extends ActorRef , Runnable {

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private var value:Option[Any] = None
  private var end = false

  override def run() : Unit = {
    try {
      lock.lock()

      next() // get first
      while (!end){
        if (value.isDefined){
          val v = value.get
          fn.accept(v)

          value = None
          next()
        }

        condition.await()
      }

    } finally {
      lock.unlock()
    }
  }

  private def next(): Unit = {
    prev.ask(HasNext).thenAccept(this )
  }

  override def accept(t: ActorMessage): Unit = {
    try {
      lock.lock()
      t.value match {
        case Next(v) =>
          if(value.isDefined){
            throw new IllegalStateException("value.isDefined")
          }
          value = Some(v)
        case End =>
          end = true
      }
      condition.signalAll()
    }finally {
      lock.unlock()
    }
  }
}
