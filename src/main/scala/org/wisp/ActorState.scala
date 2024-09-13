package org.wisp

import org.wisp.bus.FailedMessage

import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import scala.annotation.targetName
import scala.util.control.NonFatal

class ActorState(system:ActorRuntime, fn: ActorContext => Actor) extends ActorContext(system) {
  private val actor = fn(this)
  private val queue = actor.createQueue()

  override def create(f: ActorContext => Actor): ActorRef = {
    val ref = new ActorState(system, f)
    new ActorRef(system) {
      override def accept(msg: ActorMessage): Unit = ref.accept(msg)

      @targetName("send")
      override def << (value: Any): Unit = accept(actor, value)
    }
  }

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private var running:Boolean = false

  //val actorLock = new Object

  override def messageQueueSize(): Int = {
    lock.lock()
    try {
      queue.size()
    } finally {
      lock.unlock()
    }
  }

  protected def process(m: ActorMessage): Runnable = () => {
    try {
      actor.process(new ActorRef(system) {
        override def accept(msg: ActorMessage): Unit = m.sender.accept(msg)

        @targetName("send")
        override def << (value: Any): Unit = accept(actor, value)
      }).apply(m.value)
    } catch {
      case NonFatal(exc) =>
        m match {
          case am : AskMessage =>
            am.callBack.completeExceptionally(exc)
          case _ =>
            publish(new FailedMessage(actor, m, exc))
        }
    }

    lock.lock()
    try {
      val e = queue.poll()
      condition.signalAll()
      if(e == null){
        running = false
      }else{
        system.execute(process(e))
      }
    }finally {
      lock.unlock()
    }
  }

  override def accept(msg: ActorMessage): Unit = {
    lock.lock()
    try{
      if(running){
        while (!queue.put(msg)) {
          condition.await()
        }
      }else{
        running = true
        system.execute(process(msg))
      }
    } finally {
      lock.unlock()
    }
  }

}
