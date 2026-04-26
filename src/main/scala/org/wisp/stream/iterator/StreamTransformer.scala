package org.wisp.stream.iterator

import org.wisp.stream.Source
import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object StreamTransformer {

  /**
   * Creates new stream applying `function`.
   */
  def map[F, T](stream:OperationLink[F], function: F => T)(using ExecutionContext) : StreamTransformer[F, T] = {
    flatMap(stream, i => Source( function.apply(i) ) )
  }

  /**
   * Creates new stream applying `predicate`.
   */
  def filter[F](stream: OperationLink[F], predicate: F => Boolean)(using ExecutionContext): StreamTransformer[F, F] = {
    flatMap(stream, i => { if(predicate.apply(i)) Source(i) else Source.empty } )
  }

  /**
   * Creates new stream applying `function`.
   */
  def flatMap[F, T](stream:OperationLink[F], function: F => Source[T])(using ExecutionContext): StreamTransformer[F, T] = {
    StreamTransformer(stream, { case Some(v) => function(v) case None => Source.empty } )
  }

  /**
   * Creates new stream from `zero` element applying `fold`.
   * Stream will have only one element.
   */
  def fold[F, T](stream:OperationLink[F], zero:T, fold: (T, F) => T)(using ExecutionContext): StreamTransformer[F, T] = {
    var acc = zero
    StreamTransformer(stream, {
      case Some(v) =>
        acc = fold.apply(acc, v)
        Source.empty
      case None =>
        Source(acc)
    })
  }

}

/**
 * creates new `stream` applying `collect` function
 *
 * @param stream source stream
 * @param collect function to apply to each element of the source stream. `End` of stream will be mapped to `None`
 */
class StreamTransformer[F, T](stream:OperationLink[F], collect: Option[F] => Source[T])(using ec : ExecutionContext) extends StreamLink[T], SingleNodeFlow[T]{

  override protected val lock: ReentrantLock = ReentrantLock()

  protected override val nodes:util.Queue[OperationLink[T]] = createNodes()

  protected var source: Option[Source[T]] = None
  protected var ended = false

  protected def send(source:Source[T]):Boolean = {
    var hasNext = true
    while (hasNext && !nodes.isEmpty) {
      var optVal: Option[T] = None
      try {
        optVal = source.next()
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
    hasNext
  }

  protected def call(value:Option[F]):Option[Source[T]] = {
    var opt: Option[Source[T]] = None
    try {
      val r = collect.apply(value)
      opt = Some(r)
    } catch {
      case NonFatal(ex) =>
        ec.reportFailure(ex)
    }
    opt
  }

  protected val response:StreamResponse[F] = StreamResponse(lock, StreamTransformer.this.getClass) {
    case Next(v) =>
      if (ended) throw new IllegalStateException("ended")
      if (nodes.isEmpty) throw new IllegalStateException("no workers found for " + v)
      if (source.isDefined) throw new IllegalStateException("dropped value " + v)

      val opt = call(Some(v))
      val hasNext = opt match {
        case Some(s) => send(s)
        case None => false
      }

      if (hasNext) {
        source = Some(opt.get)
      } else if (!nodes.isEmpty) {
        stream.call(HasNext).onComplete(response)
      }

    case End =>
      if (source.isDefined) throw new IllegalStateException("dropped value " + source.get)
      ended = true

      val opt = call(None)
      val hasNext = opt match {
        case Some(s) => send(s)
        case None => false
      }

      if (hasNext) {
        source = Some(opt.get)
      } else {
        sendEnd()
      }

  }

  override def apply(from: OperationLink[T]): PartialFunction[Operation[T], Unit] = {
    case HasNext =>
      if (ended && source.isEmpty) {
        from << End
      } else {
        var optVal:Option[T] = None
        if(source.isDefined){
          try {
            optVal = source.get.next()
          }catch{
            case NonFatal(ex) =>
              ec.reportFailure(ex)
          }
          if(optVal.isEmpty){
            source = None
          }
        }

        if (optVal.isDefined) {
          from << Next(optVal.get)
        } else if(ended){
          from << End
        } else {
          nodes.add(from)
          stream.call(HasNext).onComplete(response)
        }
      }

  }

}
