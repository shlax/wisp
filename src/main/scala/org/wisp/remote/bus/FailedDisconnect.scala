package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object FailedDisconnect{

  def unapply(m: FailedDisconnect): Some[(AbstractConnection,Throwable)] = Some((m.connection, m.exception))
}

class FailedDisconnect(connection: AbstractConnection, val exception: Throwable) extends ConnectionEvent(connection)
