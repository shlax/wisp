package org.wisp.remote.cluster.bus

import org.wisp.bus.Event
import org.wisp.remote.cluster.{ClusterClient, ClusterSystem}

object FailedToCloseRemoteManager{

  def unapply(m: FailedToCloseRemoteManager): Some[(ClusterSystem, Throwable)] = Some(m.clusterSystem, m.exception)
}

class FailedToCloseRemoteManager(val clusterSystem:ClusterSystem, val exception: Throwable) extends Event{

  override def toString = s"FailedToCloseRemoteManager($clusterSystem, $exception)"
}
