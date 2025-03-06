package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}
import org.wisp.lock.*
import org.wisp.stream.Sink

import java.util.concurrent.locks.Condition
import scala.concurrent.ExecutionContext

class RunnableSink[T](prev:ActorLink, sink:Sink[T])(using executor: ExecutionContext) extends StreamActorLink, Runnable{

  protected val condition: Condition = lock.newCondition()

  protected var value: Option[T] = None
  protected var exception: Option[Throwable] = None

  protected var started: Boolean = false
  protected var ended = false

  protected def next(): Unit = {
    prev.call(HasNext).onComplete(accept)
  }

  override def run(): Unit = lock.withLock {
    if (started) {
      throw new IllegalStateException("started")
    } else {
      started = true
    }

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

    flush(sink, exception)
  }

  override def accept(from: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case Next(v) =>
      if (ended) throw new IllegalStateException("ended")
      if(value.isDefined) throw new IllegalStateException("dropped value: "+v)

      value = Some(v.asInstanceOf[T])
      condition.signal()
    case End(ex) =>
      if(ex.isDefined){
        exception = ex
        condition.signal()
      }else{
        val wasEnded = ended
        ended = true
        condition.signal()
        if(wasEnded){
          throw new IllegalStateException("ended")
        }
      }
  }

}
