package org.wisp.timer

import org.wisp.{ActorRef, logger}
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{Delayed, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import scala.concurrent.duration.Duration
import java.util
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

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

  def scheduleMessage(r: ActorRef, msg: Any, delay: Duration): ScheduledFuture[Void] = {
    schedule(r, delay)(() => msg)
  }

  def schedule(r:ActorRef, delay:Duration)(msg: () => Any) : ScheduledFuture[Void] = {
    lock.lock()
    try {
      val ref = new ScheduledFutureRef
      futures.add(ref)
      val f = service.schedule((() => {
        try {
          ref.remove()
          r << msg.apply()
        } catch {
          case NonFatal(e) =>
            if(logger.isErrorEnabled) logger.error("timer[" + r + "|" + delay + "]:" + e.getMessage, e)
        }
      }): Runnable, delay.length, delay.unit)
      ref.future = Some(f)
      ref
    }finally {
      lock.unlock()
    }
  }

  def scheduleMessage(r:ActorRef, msg: Any, initialDelay:Duration, period:Duration): ScheduledFuture[Void] = {
    schedule(r, initialDelay, period)(() => msg)
  }

  def schedule(r:ActorRef, initialDelay:Duration, period:Duration)(msg: () => Any): ScheduledFuture[Void] = {
    lock.lock()
    try {
      val ref = new ScheduledFutureRef
      futures.add(ref)
      val f = service.scheduleAtFixedRate ( ( () => {
        try {
          ref.remove()
          r << msg.apply()
        }catch{
          case NonFatal(e) =>
            if(logger.isErrorEnabled)  logger.error("timer["+r+"|"+initialDelay+"|"+period+"]:"+e.getMessage, e)
        }
      } ) : Runnable,initialDelay.toNanos, period.toNanos, TimeUnit.NANOSECONDS)
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
