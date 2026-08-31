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
class RunnableSource[T](src: Source[T])(using ec : ExecutionContextExecutor)
  extends SourceFlow[T], RunnableStream[T], SingleNodeFlow[T], ExecutionFlow[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected val nodes:util.Queue[Option[T] => Unit] = createNodes()

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

        if ( lock.withLock( ended || sourceException.isDefined) ) {
          a.apply(None)
        } else {
          var n: Option[T] = None

          try {
            n = src.next()
          } catch {
            case NonFatal(ex) =>
              lock.withLock { sourceException = Some(ex) }
              ec.reportFailure(ex)
          }

          n match {
            case sv : Some[T] =>
              a.apply(sv)
            case None =>
              lock.withLock { ended = true }
              a.apply(None)
          }
        }

        a = lock.withLock( nodes.poll() )

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

  override def nextWithLock(sender: Option[T] => Unit): Unit = {
    if (ended || sourceException.isDefined) {
      sender.apply(None)
    } else {
      nodes.add(sender)
      condition.signal()
    }
  }

}
