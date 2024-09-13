package org.wisp.remote.cluster.bus

import org.wisp.remote.ObjectId
import org.wisp.remote.cluster.{ClusterClient, RemoteManager}

object ClosedConnectedClusterClient{

  def unapply(m: ClosedConnectedClusterClient): Some[(ObjectId, ClusterClient, Option[Throwable])] = Some((m.id, m.clusterClient, m.exception))
}

class ClosedConnectedClusterClient(val id:ObjectId, val clusterClient:ClusterClient, val exception: Option[Throwable])
