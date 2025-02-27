package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}
import org.wisp.lock.*

import java.util
import java.util.concurrent.locks.Condition

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class ForEachSink[F, T](src:Source[F], sink:Sink[T])(link: ActorLink => ActorLink)(implicit executor: ExecutionContext)
  extends StreamActorLink, ActorLink, Runnable {

  protected val nodes: util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected val condition: Condition = lock.newCondition()
  protected val prev: ActorLink = link.apply(this)

  protected var exception: Option[Throwable] = None
  protected var value: Option[T] = None

  protected var inputEnded = false
  protected var ended = false

  protected def next(): Unit = {
    prev.ask(HasNext).future.onComplete(accept)
  }

  override def run(): Unit = lock.withLock {
    next()

    while (!ended && exception.isEmpty) {

      for (v <- value) {
        value = None
        sink.accept(v)
        next()
      }

      var a = nodes.poll()
      while (a != null) {
        if (inputEnded) {
          a << End()
        } else {
          var n: Option[F] = None
          if (exception.isEmpty) {
            try {
              n = src.next()
            } catch {
              case NonFatal(ex) =>
                exception = Some(ex)
            }
          }
          if (exception.isDefined) {
            a << End(exception)
          } else {
            n match {
              case Some(v) =>
                a << Next(v)
              case None =>
                inputEnded = true
                a << End()
            }
          }
        }
        a = nodes.poll()
      }

      if (!ended && exception.isEmpty) {
        condition.await()
      }
    }

    flush(sink, exception)

  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if(exception.isDefined){
        sender << End(exception)
      }else {
        if (inputEnded) {
          sender << End()
        } else {
          nodes.add(sender)
          condition.signal()
        }
      }

    case Next(v) =>
      if(ended) throw new IllegalStateException("ended")
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
