package org.wisp.stream.iterator

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContextExecutor

/**
 * Prefetch element from `stream`
 * @param size maximum no of elements to prefetch
 */
class StreamBuffer[T](stream:StreamFlow[T], size:Int)(using ExecutionContextExecutor) extends StreamHandler[T], SingleNodeFlow[T], SynchronizedFlow[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected val queue:util.Queue[T] = createQueue()

  protected def createQueue(): util.Queue[T] = {
    util.LinkedList[T]()
  }

  protected override val nodes: util.Queue[Response[T] => Unit] = createNodes()

  protected var requested = false
  protected var ended = false

  protected def next(): Unit = {
    if(!ended && !requested){
      if (queue.size() < size) {
        requested = true
        stream.next(this)
      }
    }
  }

  override def nextWithLock(sender: Response[T] => Unit): Unit = {
    val e = queue.poll()
    if (e == null) {
      if (ended) {
        sender.apply(End)
      } else {
        nodes.add(sender)
        next()
      }
    } else {
      sender.apply(Next(e))
      next()
    }
  }

  override def applyWithLock(t: Response[T]): Unit = t match {
    case Next(v) =>
      if(ended){
        throw new IllegalStateException("ended")
      }
      requested = false

      val n = nodes.poll()
      if (n == null) {
        queue.add(v)
      } else {
        n.apply(Next(v))
      }

      next()

    case End =>
      if(ended) throw new IllegalStateException("ended")

      requested = false
      ended = true

      if (queue.isEmpty) {
        sendEnd()
      }

  }

}
