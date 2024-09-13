package org.wisp.remote.cluster.bus

import org.wisp.bus.Event
import org.wisp.remote.ObjectId
import org.wisp.remote.cluster.ClusterClient

object AlreadyRemovedClusterClient{

  def unapply(m: AlreadyRemovedClusterClient): Some[(ClusterClient, ObjectId)] = Some((m.clusterClient, m.id))
}

class AlreadyRemovedClusterClient(val clusterClient:ClusterClient, val id:ObjectId) extends Event{

  override def toString = s"AlreadyRemovedFromRemoteManager($clusterClient, $id)"
}