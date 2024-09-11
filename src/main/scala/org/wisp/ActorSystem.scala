package org.wisp

import java.util.concurrent.*

class ActorSystem(virtual:Boolean = true) extends ActorRuntime, Executor, AutoCloseable{

  override def create(fn: ActorContext => Actor): ActorRef = new ActorState(this, fn)

  protected def createExecutor(): ExecutorService = if(virtual) Executors.newVirtualThreadPerTaskExecutor() else Executors.newWorkStealingPool()
  private val executor = createExecutor()

  override def execute(actorState: Runnable): Unit = {
    executor.execute(actorState)
  }

  override def close(): Unit = {
    //if(!executor.isShutdown) executor.shutdown()
    executor.close()
  }

}
