package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.{AbstractActor, ActorLink, ActorScheduler}
import org.wisp.stream.iterator.message.*

import java.util
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object StreamWorker {

  /** creates new `stream` applying `map` function */
  def map[F, T](stream:ActorLink, inbox:ActorScheduler, map: F => T)(using executor: ExecutionContext) : StreamWorker[F, T] = {
    StreamWorker(stream, inbox, i => Source(map.apply(i)) )
  }

  def filter[F](stream: ActorLink, inbox: ActorScheduler, filter: F => Boolean)(using executor: ExecutionContext): StreamWorker[F, F] = {
    StreamWorker(stream, inbox, i => { if(filter.apply(i)) Source(i) else Source.empty } )
  }

  /** creates new `stream` applying `flatMap` function */
  def flatMap[F, T](stream:ActorLink, inbox:ActorScheduler, flatMap: F => Source[T])(using executor: ExecutionContext): StreamWorker[F, T] = {
    StreamWorker(stream, inbox, flatMap)
  }

}

/** creates new `stream` applying `flatMap` function */
class StreamWorker[F, T](stream:ActorLink, inbox:ActorScheduler, flatMap: F => Source[T])(using executor: ExecutionContext) extends AbstractActor(inbox), StreamException{

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
        val r = flatMap.apply(v.asInstanceOf[F])
        opt = Some(r)
      }catch{
        case NonFatal(ex) =>
          exception = Some(ex)
      }

      if(exception.isDefined){
        sendEnd()
      }else {
        var hasNext = true
        while (exception.isEmpty && hasNext && !nodes.isEmpty) {
          var optVal:Option[T] = None
          try{
            optVal = opt.get.next()
          }catch{
            case NonFatal(ex) =>
              exception = Some(ex)
          }

          optVal match {
            case Some(v) =>
              val n = nodes.poll()
              n << Next(v)
            case None =>
              hasNext = false
          }
        }

        if(exception.isDefined){
          sendEnd()
        }else {
          if (hasNext) {
            source = Some(opt.get)
          } else if (!hasNext && !nodes.isEmpty) {
            stream.call(HasNext).onComplete(accept)
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
          if(source.isDefined){
            try {
              v = source.get.next()
            }catch{
              case NonFatal(ex) =>
                exception = Some(ex)
            }
            if(v.isEmpty){
              source = None
            }
          }

          if (exception.isDefined) {
            from << End(exception)
            sendEnd()
          } else {
            if (v.isDefined) {
              from << Next(v.get)
            } else {
              nodes.add(from)
              stream.call(HasNext).onComplete(accept)
            }
          }

        }
      }

    case End(ex) =>
      if(ex.isDefined) exception = ex
      else ended = true

      sendEnd()

      if(source.isDefined) throw new IllegalStateException("dropped value " + source.get)
  }

}
