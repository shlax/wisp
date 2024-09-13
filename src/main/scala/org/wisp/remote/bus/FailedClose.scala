package org.wisp.remote.bus

import org.wisp.remote.AbstractConnection

object FailedClose{

  def unapply(m: FailedClose): Some[(AbstractConnection, Throwable)] = Some((m.connection, m.exception))
}

class FailedClose(conn: AbstractConnection, val exception: Throwable) extends ConnectionEvent(conn){

  override def toString = s"FailedClose($connection, $exception)"
}
