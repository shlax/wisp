package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*
import org.wisp.utils.lock.*

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class StreamSource[T](src:Source[T])(using ec : ExecutionContext) extends SourceActorLink {

  protected var ended = false

  protected var sourceException:Option[Throwable] = None

  override def failOn(e: Throwable): this.type = lock.withLock {
    sourceException = Some(e)
    this
  }

  override def apply(sender: ActorLink): PartialFunction[Operation, Unit] = {
    case HasNext =>
      if(sourceException.isDefined){
        sender << End
      }else {
        if (ended) {
          sender << End
        } else {
          var n:Option[T] = None
          try{
            n = src.next()
          }catch{
            case NonFatal(e)=>
              sourceException = Some(e)
              ec.reportFailure(e)
          }

          if(sourceException.isDefined){
            sender << End
          }else {
            if (n.isDefined) {
              sender << Next(n.get)
            } else {
              ended = true
              sender << End
            }
          }
        }
      }
  }

}
