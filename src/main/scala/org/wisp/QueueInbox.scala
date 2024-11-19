package org.wisp

import java.util
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.util.concurrent.locks.ReentrantLock
import scala.util.control.NonFatal

class QueueInbox(system: ActorSystem, val inboxCapacity:Int) extends Inbox {

  protected class ActorMessage(val actor: Actor, val message: Message)

  protected def createQueue(): BlockingQueue[ActorMessage] = {
    new LinkedBlockingQueue[ActorMessage](inboxCapacity)
  }

  private val queue: BlockingQueue[ActorMessage] = createQueue()

  private val lock = new ReentrantLock()
  private var running:Boolean = false

  protected def pull(): Option[ActorMessage] = {
    lock.lock()
    try {
      val n = Option(queue.poll())
      if (n.isEmpty) running = false
      n
    } finally {
      lock.unlock()
    }
  }

  override def add(actor: Actor, message: Message): Unit = {
    lock.lock()
    try{
      queue.put(ActorMessage(actor, message))
      if(!running){
        running = true

        system.execute(() => {
          try {
            var next = pull()
            while(next.isDefined){
              val n = next.get
              actor.accept(n.actor).apply(n.message)
              next = pull()
            }
          } catch {
            case NonFatal(e) =>
              system.handle(actor, message, e)
          }
        })
      }
    }finally {
      lock.unlock()
    }
  }

}
