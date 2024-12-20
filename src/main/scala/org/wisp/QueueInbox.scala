package org.wisp

import java.util
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.util.concurrent.locks.ReentrantLock
import scala.annotation.targetName
import scala.compiletime.uninitialized
import scala.util.control.NonFatal

class QueueInbox[T <: Actor](override val system: ActorSystem, val inboxCapacity:Int, fn: ActorCreator[T]) extends Inbox {

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
          try {
            var next = pull()
            while(next.isDefined){
              val n = next.get
              actor.accept(new ActorRef(system){
                  @targetName("send")
                  override def <<(v: Any): Unit = accept(Message(actor, v))
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
