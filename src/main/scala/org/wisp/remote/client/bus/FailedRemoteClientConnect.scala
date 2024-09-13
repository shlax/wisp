package org.wisp.remote.client.bus

import org.wisp.bus.Event
import org.wisp.remote.client.RemoteClient

object FailedRemoteClientConnect{

  def unapply(m: FailedRemoteClientConnect): Some[(RemoteClient, Throwable)] = Some((m.remoteClient, m.exception))
}

class FailedRemoteClientConnect(val remoteClient: RemoteClient, val exception: Throwable) extends Event{
  override def stackTrace: Option[Throwable] = Some(exception)
  
  override def toString = s"FailedRemoteClientConnect($remoteClient, $exception)"
}
