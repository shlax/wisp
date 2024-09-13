package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object ConnectionEvent{

  def unapply(m: ConnectionEvent[?]): Some[AbstractConnection] = Some(m.connection)

}

class ConnectionEvent[T <: AbstractConnection](val connection: T)
