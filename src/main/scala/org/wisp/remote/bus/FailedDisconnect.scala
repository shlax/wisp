package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object FailedDisconnect{

  def unapply(m: FailedDisconnect): Some[(AbstractConnection,Throwable)] = Some((m.connection, m.exception))
}

class FailedDisconnect(conn: AbstractConnection, val exception: Throwable) extends ConnectionEvent(conn){
  override def stackTrace: Option[Throwable] = Some(exception)
  
  override def toString = s"FailedDisconnect($connection, $exception)"
}
