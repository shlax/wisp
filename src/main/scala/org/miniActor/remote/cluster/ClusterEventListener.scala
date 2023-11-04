package org.miniActor.remote.cluster

import org.miniActor.remote.{ObjectId, RemoteContext}

trait ClusterEventListener {

  def added(uuid: ObjectId, rc:RemoteContext): Unit = {}

  def closed(uuid: ObjectId, rc:RemoteContext): Unit = {}

  def add(l:ClusterEventListener):ClusterEventListener = new ClusterEventListener {

    override def added(uuid: ObjectId, rc: RemoteContext): Unit = {
      ClusterEventListener.this.added(uuid, rc)
      l.added(uuid, rc)
    }

    override def closed(uuid: ObjectId, rc: RemoteContext): Unit = {
      ClusterEventListener.this.closed(uuid, rc)
      l.closed(uuid, rc)
    }

  }

}
