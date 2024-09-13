package org.wisp.remote.client.bus

import org.wisp.ActorMessage
import org.wisp.bus.Event
import org.wisp.remote.ObjectId
import org.wisp.remote.codec.RemoteResponse

object UndeliveredRemoteResponse{

  def unapply(m: UndeliveredRemoteResponse): Some[RemoteResponse] = Some(m.remoteResponse)
}

class UndeliveredRemoteResponse(val remoteResponse:RemoteResponse) extends Event{

  override def toString = s"UndeliveredRemoteResponse($remoteResponse)"
}