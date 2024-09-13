package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object FailedClose{

  def unapply(m: FailedClose): Some[(AbstractConnection, Throwable)] = Some((m.connection, m.exception))
}

class FailedClose(connection: AbstractConnection, val exception: Throwable) extends ConnectionEvent(connection)
