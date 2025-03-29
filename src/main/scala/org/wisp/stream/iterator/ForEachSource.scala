package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*

import java.util
import org.wisp.lock.*

import java.util.concurrent.locks.Condition
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class ForEachSource[T](src:Source[T])(using executor: ExecutionContext) extends SourceActorLink, ForEachStream{

  protected val nodes:util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected val condition: Condition = lock.newCondition()

  protected var exception: Option[Throwable] = None
  protected var ended = false

  override def failOn(e:Throwable):this.type = lock.withLock {
    exception = Some(e)
    condition.signal()
    this
  }

  override def run():Unit = lock.withLock {

    while (!ended && exception.isEmpty){

      var a = nodes.poll()
      while (a != null) {
        var n: Option[T] = None
        if (!ended && exception.isEmpty) {
          try {
            n = src.next()
          } catch {
            case NonFatal(ex) =>
              exception = Some(ex)
          }
        }

        if(ended || exception.isDefined){
          a << End(exception)
        }else{
          n match {
            case Some(v) =>
              a << Next(v)
            case None =>
              ended = true
              a << End()
          }
        }

        a = nodes.poll()
      }

      if(!ended && exception.isEmpty){
        condition.await()
      }

    }
  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if (ended) {
        sender << End(exception)
      } else {
        nodes.add(sender)
        condition.signal()
      }
  }

}
