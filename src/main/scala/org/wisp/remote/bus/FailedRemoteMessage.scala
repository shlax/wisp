package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object FailedRemoteMessage{

  def unapply(m: FailedRemoteMessage): Some[(AbstractConnection, Any, Throwable)] = Some((m.connection, m.message, m.exception))

}

class FailedRemoteMessage(connection: AbstractConnection, val message: Any, val exception: Throwable) extends ConnectionEvent(connection)
