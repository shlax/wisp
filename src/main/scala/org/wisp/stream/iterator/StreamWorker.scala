package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.utils.lock.withLock
import org.wisp.{AbstractActor, ActorLink, ActorScheduler, Message}

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

object StreamWorker {

  /**
   * creates new `stream` applying `map` function
   */
  def map[F, T](stream:ActorLink[Operation[F]], map: F => T)(using ExecutionContext) : StreamWorker[F, T] = {
    StreamWorker(stream, i => Source(map.apply(i)) )
  }

  /**
   * creates new `stream` applying `filter` function
   */
  def filter[F](stream: ActorLink[Operation[F]], filter: F => Boolean)(using ExecutionContext): StreamWorker[F, F] = {
    StreamWorker(stream, i => { if(filter.apply(i)) Source(i) else Source.empty } )
  }

  /**
   * creates new `stream` applying `flatMap` function
   */
  def flatMap[F, T](stream:ActorLink[Operation[F]], flatMap: F => Source[T])(using ExecutionContext): StreamWorker[F, T] = {
    StreamWorker(stream, flatMap)
  }

}

/**
 * creates new `stream` applying `flatMap` function
 */
class StreamWorker[F, T](stream:ActorLink[Operation[F]], flatMap: F => Source[T])(using ec : ExecutionContext)
  extends StreamActorLink[T], SingleNodeFlow[T]{

  override protected val lock: ReentrantLock = ReentrantLock()

  protected override val nodes:util.Queue[ActorLink[Operation[T]]] = createNodes()

  protected var source: Option[Source[T]] = None
  protected var ended = false

  protected val responseHandler:StreamResponse[F] = new StreamResponse[F](lock) {
    override def accept: PartialFunction[Response[F], Unit] = {
      case Next(v) =>
        if (ended) throw new IllegalStateException("ended")
        if (nodes.isEmpty) throw new IllegalStateException("no workers found for " + v)
        if (source.isDefined) throw new IllegalStateException("dropped value " + v)

        var opt: Option[Source[T]] = None
        try {
          val r = flatMap.apply(v)
          opt = Some(r)
        } catch {
          case NonFatal(ex) =>
            ec.reportFailure(ex)
        }

        var hasNext = true
        while (hasNext && !nodes.isEmpty) {
          var optVal: Option[T] = None
          try {
            optVal = opt.get.next()
          } catch {
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
          stream.call(HasNext).onComplete(responseHandler)
        }

      case End =>
        if (source.isDefined) throw new IllegalStateException("dropped value " + source.get)

        ended = true
        sendEnd()

    }
  }

  override def apply(from: ActorLink[Operation[T]]): PartialFunction[Operation[T], Unit] = {

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
          stream.call(HasNext).onComplete(responseHandler)
        }
      }

  }

}
