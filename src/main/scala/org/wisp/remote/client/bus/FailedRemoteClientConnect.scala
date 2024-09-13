package org.wisp.remote.client.bus

import org.wisp.remote.client.RemoteClient

object FailedRemoteClientConnect{

  def unapply(m: FailedRemoteClientConnect): Some[(RemoteClient, Throwable)] = Some((m.remoteClient, m.exception))
}

class FailedRemoteClientConnect(val remoteClient: RemoteClient, val exception: Throwable)
