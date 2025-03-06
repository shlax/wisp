package org.wisp

import org.wisp.exceptions.ProcessingException

import java.util
import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.annotation.targetName
import scala.util.control.NonFatal
import org.wisp.lock.*

import scala.concurrent.ExecutionContext

/** Inbox backed by [[java.util.LinkedList]] witch block thread calling [[add]] when `inboxCapacity` is reached */
class QueueInbox[T <: Actor](inboxCapacity:Int, fn: Inbox => T)(using executor: ExecutionContext) extends Inbox {

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

  override def add(message: Message): Unit = lock.withLock {
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
            try {
              actor.accept(new ActorLink{
                  @targetName("send")
                  override def <<(v: Any): Unit = accept(Message(actor, v))
                  override def accept(t: Message): Unit = n.sender.accept(t)
                }).apply(n.value)
            } catch {
              case NonFatal(e) =>
                executor.reportFailure(ProcessingException(n, actor, e))
            }
            next = pull()
          }
      })
    }
  }

}
