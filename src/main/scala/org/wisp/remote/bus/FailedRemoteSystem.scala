package org.wisp.remote.bus

import org.wisp.remote.{ClientConnection, RemoteSystem}

object FailedRemoteSystem{

  def unapply(m: FailedRemoteSystem): Some[(RemoteSystem, Throwable)] = Some((m.remoteSystem, m.exception))
}

class FailedRemoteSystem(system: RemoteSystem, val exception: Throwable) extends RemoteSystemEvent(system){
  override def stackTrace: Option[Throwable] = Some(exception)

  override def toString = s"FailedRemoteSystem($remoteSystem, $exception)"
}
