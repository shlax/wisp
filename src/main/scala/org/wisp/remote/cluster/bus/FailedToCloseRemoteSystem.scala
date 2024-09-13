package org.wisp.remote.cluster.bus

import org.wisp.remote.cluster.ClusterSystem

object FailedToCloseRemoteSystem{

  def unapply(m: FailedToCloseRemoteSystem): Some[(ClusterSystem, Throwable)] = Some(m.clusterSystem, m.exception)
}

class FailedToCloseRemoteSystem (val clusterSystem:ClusterSystem, val exception: Throwable)

