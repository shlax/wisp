package org.miniActor.stream.iterator

import org.miniActor.{ActorMessage, ActorRef}

import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

object WaitSink{

  def apply(prev:ActorRef)(fn:Consumer[Any]):WaitSink = new WaitSink(prev)(fn)

}

class WaitSink(prev:ActorRef)(fn:Consumer[Any]) extends ActorRef , Runnable {

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private var value:Option[Any] = None
  private var ended = false

  override def run() : Unit = {
    lock.lock()
    try {
      next() // get first
      while (!ended) {
        while (value.isDefined) { // next() can complete CompletableFuture( ask(HasNext) ) in current thread
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
    lock.lock()
    try {
      t.value match {
        case Next(v) =>
          if(ended) throw new IllegalStateException("ended")
          if (value.isDefined) throw new IllegalStateException("value is defined")

          value = Some(v)
        case End =>
          ended = true
      }
      condition.signalAll()
    }finally {
      lock.unlock()
    }
  }
}
