package org.wisp.remote.cluster

import org.wisp.bus.EventBus
import org.wisp.remote.{ObjectId, RemoteContext}

import java.util.function.BiConsumer

trait ClusterContext extends EventBus{

  def get(id: ObjectId): RemoteContext

  def forEach(action: BiConsumer[? >: ObjectId, ? >: RemoteContext]): Unit
}
