package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object FailedConnection {

  def unapply(m: FailedConnection): Some[(AbstractConnection, Throwable)] = Some((m.connection, m.exception))
}

class FailedConnection(conn: AbstractConnection, val exception: Throwable) extends ConnectionEvent(conn){
  override def stackTrace: Option[Throwable] = Some(exception)
  
  override def toString = s"FailedConnection($connection, $exception)"
}
