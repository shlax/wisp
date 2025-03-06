package org.wisp

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, RejectedExecutionException}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.control.NonFatal

/** Hold [[scala.concurrent.ExecutionContext]] for actors
 * @param inboxCapacity default size for inboxes
 * @param finalizeWith where the calls to [[execute]] are redirected in case [[executor]] is closed */
class ActorSystem(inboxCapacity:Int = 3, finalizeWith:Option[ExecutionContext] = Some(ExecutionContext.parasitic)) extends ExecutionContext, AutoCloseable{

  protected val executor: ExecutionContextExecutorService = createExecutor()

  /** creates virtual thread executor */
  protected def createExecutor() : ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService( Executors.newVirtualThreadPerTaskExecutor(), reportFailure )
  }

  protected val closed: AtomicBoolean = AtomicBoolean(false)

  /** in case of [[scala.util.control.NonFatal]] exception in `command` report it to [[reportFailure]] */
  protected def asRunnable(command: Runnable):Runnable = {
    () => {
      try {
        command.run()
      } catch {
        case NonFatal(ex) =>
          reportFailure(ex)
      }
    }
  }

  override def execute(command: Runnable): Unit = {
    try {
      executor.execute(asRunnable(command))
    }catch{
      case e : RejectedExecutionException =>
        if(closed.get() && finalizeWith.isDefined) {
          finalizeWith.get.execute(asRunnable(command))
        }else{
          throw e
        }
    }
  }

  override def reportFailure(cause: Throwable): Unit = {
    cause.printStackTrace()
  }

  /** give this as ExecutionContext for `fn` */
  def as[R](fn: ExecutionContext ?=> this.type => R ):R = {
    given ExecutionContext = this
    val f: this.type => R = fn
    f.apply(this)
  }

  def create[T <: Actor](fn: Inbox => T):T = {
    create(inboxCapacity, fn)
  }
  
  def create[T <: Actor](inboxSize:Int, fn: Inbox => T):T = {
    given ExecutionContext = this
    QueueInbox(inboxSize, fn).actor
  }

  override def close(): Unit = {
    closed.set(true)
    executor.close()
  }

}
