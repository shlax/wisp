package org.wisp

import org.wisp.bus.{Event, EventBus, JfrEventBus}

import java.util.concurrent.*

class ActorSystem(virtual:Boolean = true, eventBus: EventBus = new JfrEventBus) extends ActorRuntime, AutoCloseable{
  override def publish(event: Event): Unit = eventBus.publish(event)

  override def create(fn: ActorContext => Actor): ActorRef = new ActorState(this, fn)

  protected def createExecutor(virtual:Boolean): ExecutorService = if(virtual) Executors.newVirtualThreadPerTaskExecutor() else Executors.newWorkStealingPool()
  private val executor = createExecutor(virtual)

  override def execute(actorState: Runnable): Unit = {
    executor.execute(actorState)
  }

  override def close(): Unit = {
    //if(!executor.isShutdown) executor.shutdown()
    executor.close()
  }

}
