package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}
import org.wisp.lock.*

import java.util.concurrent.locks.Condition
import java.util.function.{BiConsumer, Consumer}

class RunnableSink[T](prev:ActorLink, sink:Consumer[T])
  extends StreamActorLink, BiConsumer[Message, Throwable], Runnable{

  protected val condition: Condition = lock.newCondition()

  protected var value: Option[T] = None
  protected var exception: Option[Throwable] = None

  protected var ended = false

  protected def next(): Unit = {
    prev.ask(HasNext).whenComplete(this)
  }

  override def run(): Unit = lock.withLock {
    autoClose(sink) {
      next()

      while (!ended && exception.isEmpty) {

        for (v <- value) {
          value = None
          sink.accept(v)
          next()
        }

        if (!ended && exception.isEmpty) {
          condition.await()
        }

      }

      for (e <- exception) {
        throw e
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

  override def accept(t: Message, u: Throwable): Unit = {
    if(u != null){
      lock.withLock{
        exception = Some(u)
        condition.signal()
      }
    }else{
      accept(t)
    }
  }

}
