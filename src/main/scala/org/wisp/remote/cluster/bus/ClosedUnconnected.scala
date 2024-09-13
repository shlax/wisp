package org.wisp.remote.cluster.bus

import org.wisp.ActorRef
import org.wisp.bus.Event
import org.wisp.remote.cluster.ClusterClient
import org.wisp.timer.{FailedTimer, Timer}

object ClosedUnconnected{

  def unapply(m: ClosedUnconnected): Some[ClusterClient] = Some(m.clusterClient)
}

class ClosedUnconnected (val clusterClient:ClusterClient) extends Event{

  override def toString = s"ClosedUnconnected($clusterClient)"
}
