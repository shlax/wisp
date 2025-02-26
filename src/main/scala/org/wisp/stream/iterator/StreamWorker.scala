package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.stream.Source.*
import org.wisp.{Actor, ActorLink, Inbox}
import org.wisp.stream.iterator.message.*

import java.util
import scala.util.control.NonFatal

object StreamWorker {

  def map[F, T](prev:ActorLink, inbox:Inbox)(fn: F => T) : StreamWorker[F, T] = {
    StreamWorker(prev, inbox, i => Option(fn.apply(i)).asSource )
  }

  def flatMap[F, T](prev: ActorLink, inbox: Inbox)(fn: F => Source[T]): StreamWorker[F, T] = {
    StreamWorker(prev, inbox, fn)
  }

}

class StreamWorker[F, T](prev:ActorLink, inbox:Inbox, fn: F => Source[T]) extends Actor(inbox), StreamException{

  protected val nodes:util.Queue[ActorLink] = createNodes()
  protected def createNodes(): util.Queue[ActorLink] = { util.LinkedList[ActorLink]() }

  protected var exception: Option[Throwable] = None
  protected var source: Option[Source[T]] = None
  protected var ended = false

  protected def sendEnd():Unit = {
    var a = nodes.poll()
    while (a != null) {
      a << End(exception)
      a = nodes.poll()
    }
  }

  override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
    case Next(v) =>
      if (ended) throw new IllegalStateException("ended")
      if (nodes.isEmpty) throw new IllegalStateException("no workers found for " + v)
      if (source.isDefined) throw new IllegalStateException("dropped value " + v)

      var opt:Option[Source[T]] = None
      try{
        val r = fn.apply(v.asInstanceOf[F])
        opt = Some(r)
      }catch{
        case NonFatal(ex) =>
          exception = Some(ex)
      }

      if(exception.isDefined){
        sendEnd()
      }else {
        var hanNext = true
        while (exception.isEmpty && hanNext && !nodes.isEmpty) {
          var optVal:Option[Option[T]] = None
          try{
            val r = opt.get.next()
            optVal = Some(r)
          }catch{
            case NonFatal(ex) =>
              exception = Some(ex)
          }

          if(exception.isEmpty) {
            optVal.get match {
              case Some(v) =>
                val n = nodes.poll()
                n << Next(v)
              case None =>
                hanNext = false
            }
          }
        }

        if(exception.isDefined){
          sendEnd()
        }else {
          if (hanNext) {
            source = Some(opt.get)
          } else if (!hanNext && !nodes.isEmpty) {
            prev.ask(HasNext).whenComplete(this)
          }
        }
      }

    case HasNext =>
      if(exception.isDefined){
        from << End(exception)
      }else {
        if (ended) {
          from << End()
        } else {
          var v:Option[T] = None
          for(i <- source){
            v = i.next()
            if(v.isEmpty) source = None
          }
          if(v.isDefined){
            from << Next(v.get)
          }else {
            nodes.add(from)
            prev.ask(HasNext).whenComplete(this)
          }
        }
      }

    case End(ex) =>
      if(ex.isDefined) exception = ex
      else ended = true

      sendEnd()
  }

}
