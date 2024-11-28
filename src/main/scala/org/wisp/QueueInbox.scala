package org.wisp

import java.util
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.util.concurrent.locks.ReentrantLock
import scala.annotation.targetName
import scala.compiletime.uninitialized
import scala.util.control.NonFatal

class QueueInbox(override val system: ActorSystem, val inboxCapacity:Int) extends Inbox {

  private var actor:Actor = uninitialized

  def init(a:Actor):Actor = {
    lock.lock()
    try {
      this.actor = a
    } finally {
      lock.unlock()
    }
    a
  }

  private val queue: BlockingQueue[Message] = createQueue()

  protected def createQueue(): BlockingQueue[Message] = {
    new LinkedBlockingQueue[Message](inboxCapacity)
  }

  private val lock = new ReentrantLock()
  private var running:Boolean = false

  protected def pull(): Option[Message] = {
    lock.lock()
    try {
      val n = Option(queue.poll())
      if (n.isEmpty) running = false
      n
    } finally {
      lock.unlock()
    }
  }

  override def add(message: Message): Unit = {
    lock.lock()
    try{
      queue.put(message)
      if(!running){
        running = true

        system.execute(() => {
          try {
            var next = pull()
            while(next.isDefined){
              val n = next.get
              actor.accept(new ActorRef(system){
                  @targetName("send")
                  override def !(v: Any): Unit = accept(Message(actor, v))
                  override def accept(t: Message): Unit = n.from.accept(t)
                }).apply(n.message)
              next = pull()
            }
          } catch {
            case NonFatal(e) =>
              system.handle(message, Some(actor), Some(e))
          }
        })
      }
    }finally {
      lock.unlock()
    }
  }

}
