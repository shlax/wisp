package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object UndeliveredRemoteMessage{

  def unapply(m: UndeliveredRemoteMessage): Some[(AbstractConnection, Any)] = Some((m.connection, m.message))

}

class UndeliveredRemoteMessage(conn: AbstractConnection, val message: Any) extends ConnectionEvent(conn){

  override def toString = s"UndeliveredRemoteMessage($connection, $message)"
}
