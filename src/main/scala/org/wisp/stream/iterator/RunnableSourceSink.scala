package org.wisp.stream.iterator

import org.wisp.stream.{Sink, Source}
import org.wisp.utils.lock.*
import java.util
import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContextExecutor
import scala.util.control.NonFatal

class RunnableSourceSink[F, T](src:Source[F], override val sink:Sink[T])(link: RunnableSourceSink[F, T] => StreamFlow[T])(using ec : ExecutionContextExecutor)
  extends SourceFlow[F], RunnableStream[F], SingleNodeFlow[F], SinkExecution[T], StreamHandler[T], ExecutionFlow[F] {

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected val nodes: util.Queue[Response[F] => Unit] = createNodes()

  protected val condition: Condition = lock.newCondition()
  protected val prev: StreamFlow[T] = link.apply(this)

  protected var started: Boolean = false

  protected var srcEnded = false
  protected var dstEnded = false

  protected var value: Option[T] = None

  /**
   * should not to be called inside lock
   */
  protected def next(): Unit = {
    prev.next(this)
  }

  protected var sourceException: Option[Throwable] = None

  override def failOn(e: Throwable): this.type = lock.withLock {
    sourceException = Some(e)
    condition.signal()
    this
  }

  protected var sinkException: Option[Throwable] = None

  protected override def onSinkException(t: Throwable): Unit ={
    lock.withLock{ sinkException = Some(t) }
    ec.reportFailure(t)
  }

  override def run(): Unit = {
    lock.withLock {
      if (started) {
        throw new IllegalStateException("started")
      } else {
        started = true
      }
    }

    next()

    var ended: Boolean = lock.withLock(dstEnded)

    while (!ended) {

      val actValue:Option[T] = lock.withLock {
        val tmp = value
        value = None
        tmp
      }

      for (v <- actValue) {
        tryApply(v)
        next()
      }

      lock.withLock {
        var a = nodes.poll()
        while (a != null) {
          if (srcEnded) {
            a.apply(End)
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
              a.apply(End)
            } else {
              n match {
                case Some(v) =>
                  a.apply(Next(v))
                case None =>
                  srcEnded = true
                  a.apply(End)
              }
            }
          }
          a = nodes.poll()
        }

        if (!dstEnded && value.isEmpty) {
          condition.await()
        }

        ended = dstEnded
      }

    }

    sink.complete()

    lock.withLock {
      for (e <- sourceException) throw e
      for (e <- sinkException) throw e
    }

  }

  protected def applyWithLock(rv:Response[T]): Unit = rv match {
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

  override def nextWithLock(sender: Response[F] => Unit): Unit = {
    if(sourceException.isDefined || srcEnded){
      sender.apply(End)
    }else {
      nodes.add(sender)
      condition.signal()
    }
  }

}
