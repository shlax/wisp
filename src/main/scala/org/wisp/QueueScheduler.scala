package org.wisp

import org.wisp.exceptions.ProcessingException
import java.util
import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.annotation.targetName
import scala.util.control.NonFatal
import org.wisp.utils.lock.*
import scala.concurrent.ExecutionContext

/**
 * [[ActorScheduler]] backed by [[java.util.LinkedList]] witch will block thread calling [[schedule]] when `inboxCapacity` is reached
 */
class QueueScheduler[V, T <: Actor[V]](inboxCapacity:Int, fn: ActorScheduler => T)(using executor: ExecutionContext) extends ActorScheduler {

  val actor:T = fn.apply(this)

  protected val queue: util.Queue[Message] = createQueue(inboxCapacity)
  protected def createQueue(capacity:Int): util.Queue[Message] = { util.LinkedList[Message]() }

  protected val lock:ReentrantLock = ReentrantLock()
  protected val cnd: Condition = lock.newCondition()

  protected var running = false

  protected def pull(): Option[Message] = lock.withLock {
    val n = Option(queue.poll())
    if (n.isEmpty){
      running = false
    }else{
      cnd.signal()
    }
    n
  }

  override def schedule(message: Message): Unit = lock.withLock {
    while (queue.size() >= inboxCapacity){
      cnd.await()
    }
    queue.add(message)
    if(!running){
      running = true

      executor.execute(() => {
          var next = pull()
          while(next.isDefined){
            val n = next.get
            n.process(actor.getClass) {
              try {
                actor.apply(new ActorLink {
                  @targetName("send")
                  override def <<(v: Any): Unit = apply(Message(actor, v))

                  override def apply(t: Message): Unit = n.sender.apply(t)
                }).apply(n.value.asInstanceOf[V])
              } catch {
                case NonFatal(e) =>
                  executor.reportFailure(ProcessingException(n, actor, e))
              }
            }
            next = pull()
          }
      })
    }
  }

}
