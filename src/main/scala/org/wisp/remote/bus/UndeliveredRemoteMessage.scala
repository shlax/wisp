package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object UndeliveredRemoteMessage{

  def unapply(m: UndeliveredRemoteMessage): Some[(AbstractConnection, Any)] = Some((m.connection, m.message))

}

class UndeliveredRemoteMessage(connection: AbstractConnection, val message: Any) extends ConnectionEvent(connection)
