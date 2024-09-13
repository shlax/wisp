package org.wisp.remote.bus

import org.wisp.remote.{AbstractConnection, RemoteSystem}

object RemoteSystemEvent{

  def unapply(m: RemoteSystemEvent): Some[RemoteSystem] = Some(m.remoteSystem)
}

class RemoteSystemEvent(val remoteSystem: RemoteSystem)
