package org.wisp.remote

import org.wisp.remote.exceptions.RemoteAskException
import org.wisp.{ActorLink, Message}

import java.net.SocketAddress
import scala.concurrent.Future

class RemoteLink[T](c: UdpClient[T], adr:SocketAddress) extends ActorLink{

  override def accept(t: Message): Unit = {
    c.send(adr, t.value.asInstanceOf[T])
  }

  override def call(v:Any) : Future[Message] = {
    throw RemoteAskException(v)
  }
  
}
