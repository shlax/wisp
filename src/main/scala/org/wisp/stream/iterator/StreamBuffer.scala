package org.wisp.stream.iterator

import org.wisp.Link
import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext

/**
 * Prefetch element from `stream`
 * @param size maximum no of elements to prefetch
 */
class StreamBuffer[T](stream:Link[Operation[T], Operation[T]], size:Int)(using ExecutionContext) extends StreamLink[T], SingleNodeFlow[T]{

  protected override val lock:ReentrantLock = new ReentrantLock()

  protected val queue:util.Queue[T] = createQueue()

  protected def createQueue(): util.Queue[T] = {
    util.LinkedList[T]()
  }

  protected override val nodes: util.Queue[Link[Operation[T], Operation[T]]] = createNodes()

  protected var requested = false
  protected var ended = false

  protected def next(): Unit = {
    if(!ended && !requested){
      val req = if(requested) 1 else 0
      if (queue.size() + req < size) {
        requested = true
        stream.call(HasNext).onComplete(apply)
      }
    }
  }

  override def apply(sender: Link[Operation[T], Operation[T]]): PartialFunction[Operation[T], Unit] = {
    case HasNext =>
      val e = queue.poll()
      if (e == null) {
        if (ended) {
          sender << End
        } else {
          nodes.add(sender)
          next()
        }
      } else {
        sender << Next(e)
        next()
      }

    case Next(v) =>
      if(ended){
        throw new IllegalStateException("ended")
      }
      requested = false

      val n = nodes.poll()
      if (n == null) {
        queue.add(v)
      } else {
        n << Next(v)
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
