package org.wisp.remote.bus

import org.wisp.remote.{ClientConnection, RemoteSystem}

object FailedRemoteSystem{

  def unapply(m: FailedRemoteSystem): Some[(RemoteSystem, Throwable)] = Some((m.remoteSystem, m.exception))
}

class FailedRemoteSystem(remoteSystem: RemoteSystem, val exception: Throwable) extends RemoteSystemEvent(remoteSystem)
