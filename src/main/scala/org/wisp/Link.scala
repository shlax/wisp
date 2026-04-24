package org.wisp

import org.wisp.exceptions.UndeliveredException
import scala.annotation.targetName
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * Adds callback functionality to [[Consumer]]
 */
@FunctionalInterface
trait Link[-T, +R] extends Consumer[Message[T, R]]{

  /**
   * Sends a one-way asynchronous message
   */
  @targetName("send")
  def << (v:T) : Unit = {
    val msg = Message[T, R]( v, t => { throw UndeliveredException(t) })
    apply(msg)
  }

  /**
   * Sends an asynchronous message and reply [[Message]] can be obtained through returned future
   */
  def call(v:T) : Future[Message[R, T]] = {
    val cf = Promise[Message[R, T]]()
    val msg = Message[T, R]( v, t => {
        if (!cf.trySuccess(t) ) throw UndeliveredException(t)
      })
    apply(msg)
    cf.future
  }

  /**
   * Sends an asynchronous message and reply value can be obtained through returned future
   */
  def ask(v:T)(using ExecutionContext) : Future[R] = {
    call(v).map(_.value)
  }

}
