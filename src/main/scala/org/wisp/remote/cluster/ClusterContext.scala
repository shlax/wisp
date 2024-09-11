package org.wisp.remote.cluster

import org.wisp.remote.{ObjectId, RemoteContext}

import java.util.function.BiConsumer

trait ClusterContext {

  def get(id: ObjectId): RemoteContext

  def forEach(action: BiConsumer[? >: ObjectId, ? >: RemoteContext]): Unit
}
