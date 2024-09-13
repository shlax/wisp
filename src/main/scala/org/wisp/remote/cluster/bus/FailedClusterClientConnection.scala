package org.wisp.remote.cluster.bus

import org.wisp.bus.Event
import org.wisp.remote.cluster.ClusterClient

object FailedClusterClientConnection{

  def unapply(m: FailedClusterClientConnection): Some[(ClusterClient, Throwable)] = Some((m.clusterClient, m.exception))

}

class FailedClusterClientConnection(val clusterClient:ClusterClient, val exception: Throwable) extends Event{

  override def toString = s"FailedClusterClientConnection($clusterClient, $exception)"
}