package org.qwActor

import java.util.ArrayList
import java.util.concurrent.{ConcurrentHashMap, Executor, ForkJoinPool}

trait ActorContext extends ActorRef, ActorRuntime{

  def messageQueueSize():Int

}
