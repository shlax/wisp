package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.exceptions.ExceptionHandler
import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}
import org.wisp.lock.*

import java.util.concurrent.locks.Condition
import java.util.function.Consumer

class RunnableSink[T](eh:ExceptionHandler, prev:ActorLink, sink:Consumer[T]) extends StreamActorLink, Runnable{

  protected val condition: Condition = lock.newCondition()

  protected var value: Option[T] = None
  protected var ended = false

  protected def next(): Unit = {
    prev.ask(HasNext).whenComplete(eh >> this)
  }

  override def run(): Unit = lock.withLock {
   next()

    while (!ended){
      for (v <- value) {
        value = None
        sink.accept(v)
        next()
      }

      if (!ended) {
        condition.await()
      }
    }

  }

  override def accept(from: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case Next(v) =>
      if (ended) throw new IllegalStateException("ended")
      if(value.isDefined) throw new IllegalStateException("dropped value: "+v)

      value = Some(v.asInstanceOf[T])
      condition.signal()
    case End =>
      if (ended) throw new IllegalStateException("ended")
      ended = true
      condition.signal()
  }

}
