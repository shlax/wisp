package org.wisp.remote

import org.wisp.remote.exceptions.RemoteAskException
import org.wisp.{ActorLink, Message}

import java.net.SocketAddress
import scala.concurrent.Promise

class RemoteLink(c: UdpClient, adr:SocketAddress, path:String) extends ActorLink{

  override def accept(t: Message): Unit = {
    c.send(adr, RemoteMessage(path, t.value))
  }

  override def ask(v:Any) : Promise[Message] = {
    throw RemoteAskException(v)
  }
  
}
