package org.wisp.stream.iterator

import org.wisp.exceptions.ExceptionHandler
import org.wisp.{ActorLink, Message}
import org.wisp.stream.iterator.message.*
import org.wisp.lock.*

import java.util
import java.util.function.BiConsumer
import scala.util.control.NonFatal

class StreamBuffer(eh: ExceptionHandler, prev:ActorLink, size:Int) extends StreamActorLink, ActorLink, BiConsumer[Message, Throwable]{

  protected val queue:util.Queue[Any] = createQueue()
  protected def createQueue(): util.Queue[Any] = { util.LinkedList[Any]() }

  protected val nodes: util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected var exception: Option[Throwable] = None

  protected var requested = false
  protected var ended = false

  protected def next(): Unit = {
    val req = if(requested) 1 else 0
    if (!ended && queue.size() + req < size) {
      requested = true
      prev.ask(HasNext).whenComplete(this)
    }
  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if(exception.isDefined){
        sender << End(exception)
      }else{
        val e = queue.poll()
        if (e == null) {
          if (ended) {
            sender << End(exception)
          } else {
            nodes.add(sender)
            next()
          }
        } else {
          sender << Next(e)
          next()
        }
      }

    case Next(v) =>
      if(ended) throw new IllegalStateException("ended")
      requested = false

      val n = nodes.poll()
      if (n == null) {
        queue.add(v)
      } else {
        n << Next(v)
      }

      next()

    case End(ex) =>
      val wasEnded = ended
      if(ex.isDefined) exception = ex

      requested = false
      ended = true

      if (queue.isEmpty) {
        var a = nodes.poll()
        while (a != null) {
          a << End(exception)
          a = nodes.poll()
        }
      }

      if(wasEnded){
        throw new IllegalStateException("ended")
      }

  }

}
