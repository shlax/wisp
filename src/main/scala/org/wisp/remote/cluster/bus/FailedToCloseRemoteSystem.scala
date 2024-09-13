package org.wisp.remote.cluster.bus

import org.wisp.bus.Event
import org.wisp.remote.cluster.ClusterSystem

object FailedToCloseRemoteSystem{

  def unapply(m: FailedToCloseRemoteSystem): Some[(ClusterSystem, Throwable)] = Some(m.clusterSystem, m.exception)
}

class FailedToCloseRemoteSystem (val clusterSystem:ClusterSystem, val exception: Throwable) extends Event{

  override def toString = s"FailedToCloseRemoteSystem($clusterSystem, $exception)"
}

