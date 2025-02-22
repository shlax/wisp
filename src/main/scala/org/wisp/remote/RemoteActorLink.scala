package org.wisp.remote

import org.wisp.exceptions.ExceptionHandler
import org.wisp.{ActorLink, Message}

import java.net.SocketAddress

class RemoteActorLink(c: UdpClient, adr:SocketAddress, path:String, eh: ExceptionHandler) extends ActorLink(eh){

  override def accept(t: Message): Unit = {
    c.send(adr, RemoteMessage(path, t.message))
  }

}
