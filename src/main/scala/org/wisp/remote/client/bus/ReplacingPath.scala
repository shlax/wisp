package org.wisp.remote.client.bus

import org.wisp.bus.Event
import org.wisp.remote.bus.ClosedConnectedClient
import org.wisp.remote.{ClientConnection, ObjectId}
import org.wisp.remote.client.SenderPath

object ReplacingPath{

  def unapply(m: ReplacingPath): Some[(ObjectId, SenderPath)] = Some((m.id, m.old))
}

class ReplacingPath(val id:ObjectId, val old:SenderPath) extends Event{

  override def toString = s"ReplacingPath($id, $old)"
}
