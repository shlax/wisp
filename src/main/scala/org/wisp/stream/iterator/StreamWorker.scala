package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.{ActorLink, ActorScheduler}
import org.wisp.stream.iterator.message.*

import java.util
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object StreamWorker {

  /**
   * creates new `stream` applying `map` function
   */
  def map[F, T](stream:ActorLink, inbox:ActorScheduler, map: F => T)(using ExecutionContext) : StreamWorker[F, T] = {
    StreamWorker(stream, inbox, i => Source(map.apply(i)) )
  }

  /**
   * creates new `stream` applying `filter` function
   */
  def filter[F](stream: ActorLink, inbox: ActorScheduler, filter: F => Boolean)(using ExecutionContext): StreamWorker[F, F] = {
    StreamWorker(stream, inbox, i => { if(filter.apply(i)) Source(i) else Source.empty } )
  }

  /**
   * creates new `stream` applying `flatMap` function
   */
  def flatMap[F, T](stream:ActorLink, inbox:ActorScheduler, flatMap: F => Source[T])(using ExecutionContext): StreamWorker[F, T] = {
    StreamWorker(stream, inbox, flatMap)
  }

}

/**
 * creates new `stream` applying `flatMap` function
 */
class StreamWorker[F, T](stream:ActorLink, inbox:ActorScheduler, flatMap: F => Source[T])(using ec : ExecutionContext) extends StreamActor(inbox), SingleNodeFlow{

  protected override val nodes:util.Queue[ActorLink] = createNodes()

  protected var source: Option[Source[T]] = None
  protected var ended = false

  override def apply(from: ActorLink): PartialFunction[Operation, Unit] = {
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
          ec.reportFailure(ex)
      }

      var hasNext = true
      while (hasNext && !nodes.isEmpty) {
        var optVal:Option[T] = None
        try{
          optVal = opt.get.next()
        }catch{
          case NonFatal(ex) =>
            ec.reportFailure(ex)
        }

        optVal match {
          case Some(v) =>
            val n = nodes.poll()
            n << Next(v)
          case None =>
            hasNext = false
        }
      }

      if (hasNext) {
        source = Some(opt.get)
      } else if (!hasNext && !nodes.isEmpty) {
        stream.call(HasNext).onComplete(apply)
      }

    case HasNext =>
      if (ended) {
        from << End
      } else {
        var v:Option[T] = None
        if(source.isDefined){
          try {
            v = source.get.next()
          }catch{
            case NonFatal(ex) =>
              ec.reportFailure(ex)
          }
          if(v.isEmpty){
            source = None
          }
        }

        if (v.isDefined) {
          from << Next(v.get)
        } else {
          nodes.add(from)
          stream.call(HasNext).onComplete(apply)
        }
      }

    case End =>
      if(source.isDefined) throw new IllegalStateException("dropped value " + source.get)

      ended = true
      sendEnd()

  }

}
