package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.ActorLink
import org.wisp.stream.iterator.message.*
import org.wisp.lock.*

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class StreamSource[T](src:Source[T])(using executor: ExecutionContext) extends SourceActorLink {

  protected var exception:Option[Throwable] = None
  protected var ended = false

  override def failOn(e: Throwable): StreamSource.this.type = lock.withLock {
    exception = Some(e)
    this
  }

  override def accept(sender: ActorLink): PartialFunction[IteratorMessage, Unit] = {
    case HasNext =>
      if(exception.isDefined){
        sender << End(exception)
      }else {
        if (ended) {
          sender << End()
        } else {
          var n:Option[T] = None
          try{
            n = src.next()
          }catch{
            case NonFatal(e)=>
              exception = Some(e)
          }

          if(exception.isDefined){
            sender << End(exception)
          }else {
            if (n.isDefined) {
              sender << Next(n.get)
            } else {
              ended = true
              sender << End()
            }
          }
        }
      }
  }

}
