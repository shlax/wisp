package org.wisp.remote.bus

import org.wisp.remote.RemoteSystem

object FailedCloseRemoteSystem{

  def unapply(m: FailedCloseRemoteSystem): Some[(RemoteSystem, Throwable)] = Some((m.remoteSystem, m.exception))
}

class FailedCloseRemoteSystem(system: RemoteSystem, val exception: Throwable) extends RemoteSystemEvent(system){
  override def stackTrace: Option[Throwable] = Some(exception)

  override def toString = s"FailedCloseRemoteSystem($remoteSystem, $exception)"
}
