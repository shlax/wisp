package org.wisp.remote.cluster.bus

import org.wisp.ActorMessage
import org.wisp.remote.ObjectId
import org.wisp.remote.cluster.ClusterClient

object ReplacingClusterClient{

  def unapply(m: ReplacingClusterClient): Some[(ObjectId, ClusterClient)] = Some((m.id, m.clusterClient))
}

class ReplacingClusterClient (val id:ObjectId, val clusterClient: ClusterClient)
