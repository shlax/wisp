package org.qwActor.remote

import org.qwActor.{Actor, ActorContext, ActorRef, ActorRuntime}

trait RemoteActorRuntime extends ActorRuntime, RemoteContext {

  override def create(fn: ActorContext => Actor): RemoteRef

  def close(path:Any): Option[ActorRef]
}
