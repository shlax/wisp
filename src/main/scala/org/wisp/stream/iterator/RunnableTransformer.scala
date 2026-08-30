package org.wisp.stream.iterator

import org.wisp.stream.Source
import org.wisp.utils.lock.withLock

import java.util
import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContextExecutor

object RunnableTransformer {

  /**
   * Creates new stream applying `function`.
   */
  def map[F, T](stream: StreamFlow[F], function: F => T)(using ExecutionContextExecutor): RunnableTransformer[F, T] = {
    flatMap(stream, i => Source(function.apply(i)))
  }

  /**
   * Creates new stream applying `predicate`.
   */
  def filter[F](stream: StreamFlow[F], predicate: F => Boolean)(using ExecutionContextExecutor): RunnableTransformer[F, F] = {
    flatMap(stream, i => {
      if (predicate.apply(i)) Source(i) else Source.empty
    })
  }

  /**
   * Creates new stream applying `function`.
   */
  def flatMap[F, T](stream: StreamFlow[F], function: F => () => Option[T])(using ExecutionContextExecutor): RunnableTransformer[F, T] = {
    RunnableTransformer(stream, { case Some(v) => function(v) case None => Source.empty })
  }

  /**
   * Folds the elements of `stream` using the specified associative binary `operator`.
   * The result of applying the fold `operator` between `zero` and all `stream` elements will be passed downstream as a single element.
   */
  def fold[F, T](stream: StreamFlow[F], zero: T, operator: (T, F) => T)(using ExecutionContextExecutor): RunnableTransformer[F, T] = {
    var acc = zero
    RunnableTransformer(stream, {
      case Some(v) =>
        acc = operator.apply(acc, v)
        Source.empty
      case None =>
        Source(acc)
    })
  }

}

class RunnableTransformer[F, T](stream:StreamFlow[F], override protected val collect: Option[F] => () => Option[T])(using ec : ExecutionContextExecutor)
  extends RunnableStream[T], SingleNodeFlow[T], ExecutionFlow[T], StreamHandler[F], SourceTransformer[F, T]{

  override protected val nodes: util.Queue[Option[T] => Unit] = createNodes()

  override protected val lock: ReentrantLock = new ReentrantLock()

  protected val condition: Condition = lock.newCondition()

  protected var started: Boolean = false

  protected var value: Option[F] = None
  protected var ended = false

  override def run(): Unit = {

    lock.withLock {
      if (started) {
        throw new IllegalStateException("started")
      } else {
        started = true
      }
    }

    var calledNone = false
    var src: Option[() => Option[T]] = None

    stream.next(this)

    var notEnded = lock.withLock( !ended || value.isDefined )
    
    while ( notEnded || src.isDefined || !calledNone) {

      if(src.isEmpty) {
        if( lock.withLock(value.isDefined) ) {
          val actValue = lock.withLock {
            val tmp = value
            value = None
            tmp
          }
          src = call(actValue)
        }else if(lock.withLock(ended) && !calledNone){
          calledNone = true
          src = call(None)
        }
      }

      if(src.isDefined && ! lock.withLock( nodes.isEmpty ) ){
        val source = src.get
        source.apply() match {
          case sv : Some[T] =>
            val n = lock.withLock( nodes.poll() )
            n.apply(sv)
          case None =>
            src = None
            if(! lock.withLock(ended) ) {
              stream.next(this)
            }
        }
      }

      notEnded = lock.withLock {
        if(ended){
          if(nodes.isEmpty){
            condition.await()
          }
        }else{
          if( (value.isEmpty && src.isEmpty) || nodes.isEmpty ) {
            condition.await()
          }
        }

        !ended || value.isDefined
      }

    }

    lock.withLock {
      runEnded = true
      sendEnd()
    }

  }

  protected var runEnded: Boolean = false

  override protected def applyWithLock(r: Option[F]): Unit = {
    r match {
      case sv : Some[F] =>
        if(ended){
          throw new IllegalStateException("ended")
        }
        if (value.isDefined){
          throw new IllegalStateException("dropped value: " + sv.get)
        }
        value = sv
      case None =>
        if (value.nonEmpty){
          throw new IllegalStateException("not ended")
        }
        ended = true
    }
    condition.signal()
  }

  override protected def nextWithLock(callback: Option[T] => Unit): Unit = {
    if(runEnded){
      callback.apply(None)
    }else {
      nodes.add(callback)
      condition.signal()
    }
  }

}
