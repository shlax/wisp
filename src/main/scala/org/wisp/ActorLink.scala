package org.wisp

import org.wisp.exceptions.UndeliveredException
import scala.annotation.targetName
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/** Reference to [[Actor]] */
@FunctionalInterface
trait ActorLink extends Consumer[Message]{

  /**
   * Sends a one-way asynchronous message
   */
  @targetName("send")
  def <<(v:Any) : Unit = {
    val msg = Message( t => { throw UndeliveredException(t) }, v)
    apply(msg)
  }

  /**
   * Sends an asynchronous message and reply [[Message]] can be obtained through returned future
   */
  def call(v:Any) : Future[Message] = {
    val cf = Promise[Message]()
    val msg = Message( t => { if (!cf.trySuccess(t)) throw UndeliveredException(t) }, v)
    apply(msg)
    cf.future
  }

  /**
   * Sends an asynchronous message and reply value can be obtained through returned future
   */
  def ask(v:Any)(using ExecutionContext) : Future[Any] = {
    call(v).map(_.value)
  }

  /**
   * Convert [[scala.util.Try]] to stream [[org.wisp.Consumer#apply]].
   * [[scala.util.Failure]] will be thrown as `exception`
   *
   * {{{
   *   val link:ActorLink = ????
   *   link.call(HasNext).onComplete(apply)
   * }}}
   */
  def apply(t: Try[Message]): Unit = {
    t match {
      case Success(message) =>
        apply(message)
      case Failure(exception) =>
        throw exception
    }
  }

}
