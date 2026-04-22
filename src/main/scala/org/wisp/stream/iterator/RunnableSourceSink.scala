package org.wisp.stream.iterator

import org.wisp.{ActorLink, Message}
import org.wisp.stream.{Sink, Source}
import org.wisp.utils.lock.*

import java.util
import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

class RunnableSourceSink[F, T](src:Source[F], override  val sink:Sink[T])(link: RunnableSourceSink[F, T] => ActorLink[Operation[T]])(using ec : ExecutionContext)
  extends SourceActorLink[F], RunnableStream[F], SingleNodeFlow[F], SinkExecution[T] {

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected val nodes: util.Queue[ActorLink[Operation[F]]] = createNodes()

  protected val condition: Condition = lock.newCondition()
  protected val prev: ActorLink[Operation[T]] = link.apply(this)

  protected var started: Boolean = false

  protected var srcEnded = false
  protected var dstEnded = false

  protected var value: Option[T] = None

  protected def next(): Unit = {
    prev.call(HasNext).onComplete(responseHandler)
  }

  protected var sourceException: Option[Throwable] = None

  override def failOn(e: Throwable): this.type = lock.withLock {
    sourceException = Some(e)
    condition.signal()
    this
  }

  protected var sinkException: Option[Throwable] = None

  protected override def onSinkException(t: Throwable): Unit = {
    sinkException = Some(t)
  }

  override def run(): Unit = lock.withLock {
    if (started) {
      throw new IllegalStateException("started")
    } else {
      started = true
    }

    next()

    while (!dstEnded) {

      for (v <- value) {
        value = None
        tryApply(v)
        next()
      }

      var a = nodes.poll()
      while (a != null) {
        if (srcEnded) {
          a << End
        } else {
          var n: Option[F] = None
          if (!srcEnded && sourceException.isEmpty) {
            try {
              n = src.next()
            } catch {
              case NonFatal(ex) =>
                sourceException = Some(ex)
                ec.reportFailure(ex)
            }
          }
          if (srcEnded || sourceException.isDefined) {
            a << End
          } else {
            n match {
              case Some(v) =>
                a << Next(v)
              case None =>
                srcEnded = true
                a << End
            }
          }
        }
        a = nodes.poll()
      }

      if (!dstEnded) {
        condition.await()
      }
    }

    sink.complete()

    for(e <- sourceException) throw e
    for(e <- sinkException) throw e

  }

  protected val responseHandler: StreamResponse[T] = new StreamResponse[T](lock) {
    override def apply: PartialFunction[Response[T], Unit] = {
      case Next(v) =>
        if (dstEnded) throw new IllegalStateException("ended")
        if (value.isDefined) throw new IllegalStateException("dropped value: " + v)

        value = Some(v)
        condition.signal()

      case End =>
        if (dstEnded) throw new IllegalStateException("ended")

        dstEnded = true
        condition.signal()
    }
  }

  override def apply(sender: ActorLink[Operation[F]]): PartialFunction[Operation[F], Unit] = {
    case HasNext =>
      if(sourceException.isDefined){
        sender << End
      }else {
        if (srcEnded) {
          sender << End
        } else {
          nodes.add(sender)
          condition.signal()
        }
      }
  }

}
