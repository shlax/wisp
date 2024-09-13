package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection
import org.wisp.bus.Event

object ConnectionEvent{

  def unapply(m: ConnectionEvent[?]): Some[AbstractConnection] = Some(m.connection)

}

abstract class ConnectionEvent[T <: AbstractConnection](val connection: T) extends Event
