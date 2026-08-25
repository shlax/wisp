package org.wisp.stream.iterator

import org.wisp.stream.Source
import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.ExecutionContextExecutor
import scala.util.control.NonFatal

object StreamTransformer {

  /**
   * Creates new stream applying `function`.
   */
  def map[F, T](stream:StreamFlow[F], function: F => T)(using ExecutionContextExecutor) : StreamTransformer[F, T] = {
    flatMap(stream, i => Source( function.apply(i) ) )
  }

  /**
   * Creates new stream applying `predicate`.
   */
  def filter[F](stream: StreamFlow[F], predicate: F => Boolean)(using ExecutionContextExecutor): StreamTransformer[F, F] = {
    flatMap(stream, i => { if(predicate.apply(i)) Source(i) else Source.empty } )
  }

  /**
   * Creates new stream applying `function`.
   */
  def flatMap[F, T](stream:StreamFlow[F], function: F => Source[T])(using ExecutionContextExecutor): StreamTransformer[F, T] = {
    StreamTransformer(stream, { case Some(v) => function(v) case None => Source.empty } )
  }

  /**
   * Folds the elements of `stream` using the specified associative binary `operator`.
   * The result of applying the fold `operator` between `zero` and all `stream` elements will be passed downstream as a single element.
   */
  def fold[F, T](stream:StreamFlow[F], zero:T, operator: (T, F) => T)(using ExecutionContextExecutor): StreamTransformer[F, T] = {
    var acc = zero
    StreamTransformer(stream, {
      case Some(v) =>
        acc = operator.apply(acc, v)
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
class StreamTransformer[F, T](stream:StreamFlow[F], override protected val collect: Option[F] => Source[T])(using ec : ExecutionContextExecutor)
  extends StreamHandler[F], SingleNodeFlow[T], ExecutionFlow[T], SourceTransformer[F, T]{

  override protected val lock: ReentrantLock = ReentrantLock()

  protected override val nodes:util.Queue[Response[T] => Unit] = createNodes()

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
          n.apply(Next(v))
        case None =>
          hasNext = false
      }
    }
    hasNext
  }

  protected def applyWithLock(sr:Response[F]): Unit = sr match {
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
        stream.next(this)
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

  override def nextWithLock(from: Response[T] => Unit): Unit = {
    if (ended && source.isEmpty) {
      from.apply(End)
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
        from.apply(Next(optVal.get))
      } else if(ended){
        from.apply(End)
      } else {
        nodes.add(from)
        stream.next(this)
      }
    }
  }

}
