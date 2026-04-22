package org.wisp

import org.wisp.exceptions.UndeliveredException
import scala.annotation.targetName
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * Reference to [[Actor]]
 */
@FunctionalInterface
trait Link[T, R] extends Consumer[Message[T, R]]{

  /**
   * Sends a one-way asynchronous message
   */
  @targetName("send")
  def <<(v:T) : Unit = {
    val msg = Message[T, R](t => { throw UndeliveredException(t) }, v)
    apply(msg)
  }

  /**
   * Sends an asynchronous message and reply [[Message]] can be obtained through returned future
   */
  def call(v:T) : Future[Message[R, T]] = {
    val cf = Promise[Message[R, T]]()
    val msg = Message[T, R](t => {
        if (!cf.trySuccess(t) ) throw UndeliveredException(t)
      } , v)
    apply(msg)
    cf.future
  }

  /**
   * Sends an asynchronous message and reply value can be obtained through returned future
   */
  def ask(v:T)(using ExecutionContext) : Future[R] = {
    call(v).map(_.value)
  }

  /**
   * Convert [[scala.util.Try]] to stream [[org.wisp.Consumer#apply]].
   *
   * [[scala.util.Failure]] will be thrown as `exception`
   *
   * {{{
   *   val link:ActorLink = ????
   *   link.call(HasNext).onComplete(apply)
   * }}}
   */
  def apply(t: Try[Message[T, R]]): Unit = {
    t match {
      case Success(message) =>
        message.process(Link.this.getClass) {
          apply(message)
        }
      case Failure(exception) =>
        throw exception
    }
  }

}
