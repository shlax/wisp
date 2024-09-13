package org.wisp.remote.cluster.bus

import org.wisp.bus.Event
import org.wisp.remote.ObjectId
import org.wisp.remote.cluster.ClusterClient
import org.wisp.remote.cluster.RemoteManager

object AlreadyRemovedFromRemoteManager{

  def unapply(m: AlreadyRemovedFromRemoteManager): Some[(RemoteManager, ObjectId)] = Some((m.remoteManager, m.id))
}

class AlreadyRemovedFromRemoteManager (val remoteManager:RemoteManager, val id:ObjectId) extends Event{

  override def toString = s"AlreadyRemovedFromRemoteManager($remoteManager, $id)"
}