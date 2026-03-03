package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*

import java.util
import org.wisp.utils.lock.*

import java.util.concurrent.locks.Condition
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class RunnableSource[T](src:Source[T])(using ExecutionContext) extends SourceActorLink, RunnableStream{

  protected val nodes:util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected val condition: Condition = lock.newCondition()
  
  protected var ended = false

  protected var sourceException: Option[Throwable] = None
  
  override def failOn(e:Throwable):this.type = lock.withLock {
    sourceException = Some(e)
    condition.signal()
    this
  }

  override def run():Unit = lock.withLock {

    while (!ended && sourceException.isEmpty){

      var a = nodes.poll()
      while (a != null) {
        var n: Option[T] = None
        if (!ended && sourceException.isEmpty) {
          try {
            n = src.next()
          } catch {
            case NonFatal(ex) =>
              sourceException = Some(ex)
          }
        }

        if(ended || sourceException.isDefined){
          a << End
        }else{
          n match {
            case Some(v) =>
              a << Next(v)
            case None =>
              ended = true
              a << End
          }
        }

        a = nodes.poll()
      }

      if(!ended && sourceException.isEmpty){
        condition.await()
      }

    }
    
    for(e <- sourceException) throw e
    
  }

  override def accept(sender: ActorLink): PartialFunction[Operation, Unit] = {
    case HasNext =>
      if (ended) {
        sender << End
      } else {
        nodes.add(sender)
        condition.signal()
      }
  }

}
