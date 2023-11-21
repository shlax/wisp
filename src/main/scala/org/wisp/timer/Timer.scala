package org.wisp.timer

import org.wisp.ActorRef

import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture}
import scala.concurrent.duration.Duration

class Timer extends AutoCloseable{

  protected def createScheduledExecutorService(): ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  val service : ScheduledExecutorService = createScheduledExecutorService()

  def scheduleOnce(r:ActorRef, msg:Any, delay:Duration): ScheduledFuture[_] = {
    service.schedule( ( () => {
        r << msg
    } ) : Runnable, delay.length, delay.unit)
  }

  def schedule(r:ActorRef, msg:Any, initialDelay:Duration, period:Duration): ScheduledFuture[_] = {
    if(initialDelay.unit != period.unit) throw new IllegalArgumentException("initialDelay.unit != period.unit")
    service.scheduleAtFixedRate ( ( () => {
      r << msg
    } ) : Runnable,initialDelay.length, period.length, period.unit)
  }

  override def close(): Unit = {
    //if(!service.isShutdown) service.shutdown()
    service.close()
  }

}
