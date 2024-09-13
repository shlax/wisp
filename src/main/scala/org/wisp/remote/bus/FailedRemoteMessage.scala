package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object FailedRemoteMessage{

  def unapply(m: FailedRemoteMessage): Some[(AbstractConnection, Any, Throwable)] = Some((m.connection, m.message, m.exception))

}

class FailedRemoteMessage(conn: AbstractConnection, val message: Any, val exception: Throwable) extends ConnectionEvent(conn){
  override def stackTrace: Option[Throwable] = Some(exception)
  
  override def toString = s"FailedRemoteMessage($connection, $message, $exception)"
}
