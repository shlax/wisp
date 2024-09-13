package org.wisp.remote.bus

import org.wisp.remote.{AbstractConnection, ClientConnection}

object ClosingRemoved{

  def unapply(m: ClosingRemoved): Some[ClientConnection] = Some(m.connection)
}

class ClosingRemoved(connection: ClientConnection) extends ConnectionEvent(connection)
