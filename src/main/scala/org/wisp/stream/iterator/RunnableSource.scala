package org.wisp.stream.iterator

import org.wisp.stream.Source
import java.util
import org.wisp.utils.lock.*

import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContextExecutor
import scala.util.control.NonFatal

/**
 * This class implements a stream source that can be executed on a thread to consume elements from [[Source]]
 */
class RunnableSource[T](src:Source[T])(using ec : ExecutionContextExecutor) 
  extends SourceFlow[T], RunnableStream[T], SingleNodeFlow[T], ExecutionFlow[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected val nodes:util.Queue[Response[T] => Unit] = createNodes()

  protected val condition: Condition = lock.newCondition()
  
  protected var ended = false

  protected var sourceException: Option[Throwable] = None
  
  override def failOn(e:Throwable):this.type = lock.withLock {
    sourceException = Some(e)
    condition.signal()
    this
  }

  protected var started: Boolean = false

  override def run():Unit = {
    var srcEnded: Boolean = lock.withLock {
      if (started) {
        throw new IllegalStateException("started")
      } else {
        started = true
      }

      ( ended || sourceException.isDefined ) && nodes.isEmpty
    }

    while (!srcEnded){

      var a = lock.withLock( nodes.poll() )
      while (a != null) {
        var n: Option[T] = None

        if ( lock.withLock( !ended && sourceException.isEmpty) ) {
          try {
            n = src.next()
          } catch {
            case NonFatal(ex) =>
              lock.withLock { sourceException = Some(ex) }
              ec.reportFailure(ex)
          }
        }

        a = lock.withLock {
          if (ended || sourceException.isDefined) {
            a.apply(End)
          } else {
            n match {
              case Some(v) =>
                a.apply(Next(v))
              case None =>
                ended = true
                a.apply(End)
            }
          }

          nodes.poll()
        }

      }

      srcEnded = lock.withLock {
        if (!ended && sourceException.isEmpty && nodes.isEmpty) {
          condition.await()
        }

        ( ended || sourceException.isDefined ) && nodes.isEmpty
      }

    }

    lock.withLock {
      for (e <- sourceException) throw e
    }
    
  }

  override def nextWithLock(sender: Response[T] => Unit): Unit = {
      if (ended) {
        sender.apply(End)
      } else {
        nodes.add(sender)
        condition.signal()
      }
  }

}
