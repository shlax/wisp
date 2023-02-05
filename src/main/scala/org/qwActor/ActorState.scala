package org.qwActor

import org.qwActor.{Actor, ActorContext, ActorMessage, ActorRef}

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import scala.annotation.targetName
import scala.util.control.NonFatal

class ActorState(system:Executor, fn: ActorContext => Actor) extends ActorContext {
  private val actor = fn(this)
  private val queue = actor.createQueue()

  override def create(f: ActorContext => Actor): ActorRef = {
    val ref = new ActorState(system, f)
    new ActorRef {
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
    try {
      lock.lock()
      queue.size()
    } finally {
      lock.unlock()
    }
  }

  protected def process(m: ActorMessage): Runnable = () => {
    try {
      actor.process(new ActorRef {
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
            if(logger.isErrorEnabled) logger.error("actor process("+m+") failed " + exc.getMessage, exc)
        }
    }

    try {
      lock.lock()
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
    try{
      lock.lock()
      if(running){
        while(!queue.put(msg)){
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
