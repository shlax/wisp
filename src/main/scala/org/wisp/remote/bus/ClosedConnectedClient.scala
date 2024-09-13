package org.wisp.remote.bus

import org.wisp.remote.{AbstractConnection, ClientConnection}

object ClosedConnectedClient{

  def unapply(m: ClosedConnectedClient): Some[(ClientConnection, Option[Throwable])] = Some((m.connection, m.exception))
}

class ClosedConnectedClient(conn: ClientConnection, val exception: Option[Throwable]) extends ConnectionEvent(conn){

  override def toString = s"ClosedConnectedClient($connection, $exception)"
}

