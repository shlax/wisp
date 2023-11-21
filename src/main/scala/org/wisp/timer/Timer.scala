package org.wisp.timer

import org.wisp.ActorRef

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{Delayed, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import scala.concurrent.duration.Duration
import java.util
import scala.jdk.CollectionConverters._

class Timer extends AutoCloseable{

  protected def createScheduledExecutorService(): ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  val service : ScheduledExecutorService = createScheduledExecutorService()

  private val lock = new ReentrantLock()
  private val futures = new util.HashSet[ScheduledFutureRef]()

  private class ScheduledFutureRef extends ScheduledFuture[Void]{
    var future:Option[ScheduledFuture[_]] = None

    def remove():Unit = {
      lock.lock()
      try {
        futures.remove(this)
      } finally {
        lock.unlock()
      }
    }

    override def cancel(mayInterruptIfRunning: Boolean): Boolean = {
      val res = future.get.cancel(mayInterruptIfRunning)
      remove()
      res
    }

    override def getDelay(unit: TimeUnit): Long = future.get.getDelay(unit)
    override def compareTo(o: Delayed): Int = future.get.compareTo(o)
    override def isCancelled: Boolean = future.get.isCancelled
    override def isDone: Boolean = future.get.isDone
    override def get(timeout: Long, unit: TimeUnit): Void ={
      future.get.get(timeout, unit)
      null
    }
    override def get(): Void = {
      future.get.get()
      null
    }
  }

  def scheduleOnce(r:ActorRef, msg: () => Any, delay:Duration) : ScheduledFuture[Void] = {
    lock.lock()
    try {
      val ref = new ScheduledFutureRef
      futures.add(ref)
      val f = service.schedule((() => {
        ref.remove()
        r << msg.apply()
      }): Runnable, delay.length, delay.unit)
      ref.future = Some(f)
      ref
    }finally {
      lock.unlock()
    }
  }

  def schedule(r:ActorRef, msg: () => Any, initialDelay:Duration, period:Duration): ScheduledFuture[Void] = {
    if(initialDelay.unit != period.unit) throw new IllegalArgumentException("initialDelay.unit != period.unit")
    lock.lock()
    try {
      val ref = new ScheduledFutureRef
      futures.add(ref)
      val f = service.scheduleAtFixedRate ( ( () => {
        r << msg.apply()
      } ) : Runnable,initialDelay.length, period.length, period.unit)
      ref.future = Some(f)
      ref
    } finally {
      lock.unlock()
    }
  }

  override def close(): Unit = {
    //if(!service.isShutdown) service.shutdown()
    lock.lock()
    try {
      for(f <- futures.asScala) f.cancel(false)
    }finally {
      lock.unlock()
    }
    service.close()
  }

}
