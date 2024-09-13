package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object FailedConnection {

  def unapply(m: FailedConnection): Some[(AbstractConnection, Throwable)] = Some((m.connection, m.exception))
}

class FailedConnection(connection: AbstractConnection, val exception: Throwable) extends ConnectionEvent(connection)
