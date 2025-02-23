package org.wisp

import org.wisp.exceptions.ProcessingException

import java.util
import java.util.concurrent.locks.ReentrantLock
import scala.annotation.targetName
import scala.util.control.NonFatal

class QueueInbox[T <: Actor](override val system: ActorSystem, val inboxCapacity:Int, fn: ActorFactory[T]) extends Inbox {

  val actor:T = fn.create(this)

  private val queue: util.Queue[Message] = createQueue(inboxCapacity)

  protected def createQueue(capacity:Int): util.Queue[Message] = {
    util.LinkedList[Message]()
  }

  private val lock = ReentrantLock()
  private val cnd = lock.newCondition()

  private var running:Boolean = false

  protected def pull(): Option[Message] = {
    lock.lock()
    try {
      val n = Option(queue.poll())
      if (n.isEmpty){
        running = false
      }else{
        cnd.signal()
      }
      n
    } finally {
      lock.unlock()
    }
  }

  override def add(message: Message): Unit = {
    lock.lock()
    try{
      while (queue.size() >= inboxCapacity){
        cnd.await()
      }
      queue.add(message)
      if(!running){
        running = true

        system.execute(() => {
            var next = pull()
            while(next.isDefined){
              val n = next.get
              try {
                actor.accept(new ActorLink{
                    @targetName("send")
                    override def <<(v: Any): Unit = accept(Message(actor, v))
                    override def accept(t: Message): Unit = n.sender.accept(t)
                  }).apply(n.message)
              } catch {
                case NonFatal(e) =>
                  system.handle(ProcessingException(n, actor, e))
              }
              next = pull()
            }
        })
      }
    }finally {
      lock.unlock()
    }
  }

}
