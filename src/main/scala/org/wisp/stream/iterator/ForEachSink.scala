package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.stream.{Sink, Source}
import org.wisp.stream.iterator.message.{End, HasNext, IteratorMessage, Next}
import org.wisp.lock.*

import java.util
import java.util.concurrent.locks.Condition

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class ForEachSink[F, T](src:Source[F], sink:Sink[T])(link: ActorLink => ActorLink)(using executor: ExecutionContext) extends SourceActorLink, Runnable {

  protected val nodes: util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected val condition: Condition = lock.newCondition()
  protected val prev: ActorLink = link.apply(this)

  protected var exceptionSrc: Option[Throwable] = None
  protected var exceptionDst: Option[Throwable] = None

  protected var srcEnded = false
  protected var dstEnded = false

  protected var value: Option[T] = None

  protected def next(): Unit = {
    prev.ask(HasNext).future.onComplete(accept)
  }

  override def failOn(e: Throwable): this.type = lock.withLock {
    exceptionSrc = Some(e)
    condition.signal()
    this
  }

  override def run(): Unit = lock.withLock {
    next()

    while (!dstEnded && exceptionDst.isEmpty) {

      for (v <- value) {
        value = None
        sink.accept(v)
        next()
      }

      var a = nodes.poll()
      while (a != null) {
        if (srcEnded) {
          a << End()
        } else {
          var n: Option[F] = None
          if (!srcEnded && exceptionSrc.isEmpty) {
            try {
              n = src.next()
            } catch {
              case NonFatal(ex) =>
                exceptionSrc = Some(ex)
            }
          }
          if (srcEnded || exceptionSrc.isDefined) {
            a << End(exceptionSrc)
          } else {
            n match {
              case Some(v) =>
                a << Next(v)
              case None =>
                srcEnded = true
                a << End()
            }
          }
        }
        a = nodes.poll()
      }

      if (!dstEnded && exceptionDst.isEmpty) {
        condition.await()
      }
    }

    flush(sink, exceptionDst)

  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if(exceptionSrc.isDefined){
        sender << End(exceptionSrc)
      }else {
        if (srcEnded) {
          sender << End()
        } else {
          nodes.add(sender)
          condition.signal()
        }
      }

    case Next(v) =>
      if(dstEnded) throw new IllegalStateException("ended")
      if(value.isDefined) throw new IllegalStateException("dropped value: "+v)

      value = Some(v.asInstanceOf[T])
      condition.signal()

    case End(ex) =>
      if(ex.isDefined){
        exceptionDst = ex
        condition.signal()
      }else{
        val wasEnded = dstEnded
        dstEnded = true
        condition.signal()
        if(wasEnded){
          throw new IllegalStateException("ended")
        }
      }
  }

}
