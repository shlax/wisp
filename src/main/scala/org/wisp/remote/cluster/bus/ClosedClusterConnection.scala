package org.wisp.remote.cluster.bus

import org.wisp.bus.Event
import org.wisp.remote.{ClientConnection, ObjectId}
import org.wisp.remote.cluster.ClusterClient

object ClosedClusterConnection{

  def unapply(m: ClosedClusterConnection): Some[(ClientConnection, Option[ObjectId])] = Some((m.connection, m.id))
}

class ClosedClusterConnection(val connection:ClientConnection, val id:Option[ObjectId]) extends Event{

  override def toString = s"ClosedClusterConnection($connection, $id)"
}
