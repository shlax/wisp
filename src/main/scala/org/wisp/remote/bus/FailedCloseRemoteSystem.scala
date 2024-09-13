package org.wisp.remote.bus

import org.wisp.remote.RemoteSystem

object FailedCloseRemoteSystem{

  def unapply(m: FailedCloseRemoteSystem): Some[(RemoteSystem, Throwable)] = Some((m.remoteSystem, m.exception))
}

class FailedCloseRemoteSystem(remoteSystem: RemoteSystem, val exception: Throwable) extends RemoteSystemEvent(remoteSystem)
