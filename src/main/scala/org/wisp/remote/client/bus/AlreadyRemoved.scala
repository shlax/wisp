package org.wisp.remote.client.bus

import org.wisp.ActorMessage
import org.wisp.remote.ObjectId
import org.wisp.remote.client.SenderPath

object AlreadyRemoved{

  def unapply(m: AlreadyRemoved): Some[(ObjectId, ActorMessage, Option[Throwable])] = Some((m.id, m.message, m.exception))
}

class AlreadyRemoved(val id:ObjectId, val message:ActorMessage, val exception:Option[Throwable])