package org.miniActor.remote

import org.miniActor.{Actor, ActorContext, ActorRef, ActorRuntime}

trait RemoteActorRuntime extends ActorRuntime, RemoteContext {

  override def create(fn: ActorContext => Actor): RemoteRef

  def close(path:Any): Option[ActorRef]
}
