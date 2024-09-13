package org.wisp.remote.bus

import org.wisp.remote.{AbstractConnection, ClientConnection}

object ClosingRemoved{

  def unapply(m: ClosingRemoved): Some[ClientConnection] = Some(m.connection)
}

class ClosingRemoved(conn: ClientConnection) extends ConnectionEvent(conn){

  override def toString = s"ClosingRemoved($connection)"
}
