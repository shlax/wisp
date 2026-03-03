package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*

import java.util
import scala.concurrent.ExecutionContext

/** Prefetch element from `stream`
 * @param size maximum no of elements to prefetch */
class StreamBuffer(stream:ActorLink, size:Int)(using ExecutionContext) extends StreamActorLink, ActorLink, SingleNodeFlow{

  protected val queue:util.Queue[Any] = createQueue()
  protected def createQueue(): util.Queue[Any] = { util.LinkedList[Any]() }

  protected override val nodes: util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected var requested = false
  protected var ended = false

  protected def next(): Unit = {
    if(!ended && !requested){
      val req = if(requested) 1 else 0
      if (queue.size() + req < size) {
        requested = true
        stream.call(HasNext).onComplete(accept)
      }
    }
  }

  override def accept(sender: ActorLink): PartialFunction[Operation, Unit] = {
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
      val wasEnded = ended

      requested = false
      ended = true

      if (queue.isEmpty) {
        sendEnd()
      }

      if(wasEnded){
        throw new IllegalStateException("ended")
      }

  }

}
