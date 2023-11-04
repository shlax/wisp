package org.miniActor.remote.cluster

import org.miniActor.remote.{ObjectId, RemoteContext}

import java.util.function.BiConsumer

trait ClusterContext {

  def get(id: ObjectId): RemoteContext

  def forEach(action: BiConsumer[_ >: ObjectId, _ >: RemoteContext]): Unit
}
