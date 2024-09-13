package org.wisp

import org.wisp.bus.EventBus

import java.util.concurrent.Executor

trait ActorRuntime extends Executor, EventBus{

  def create(fn: ActorContext => Actor): ActorRef

}
