package org.wisp.remote

import org.wisp.remote.exceptions.UnsupportedAskException
import org.wisp.{ActorLink, Message}

import java.net.SocketAddress
import java.util.concurrent.CompletableFuture

class RemoteLink(c: UdpClient, adr:SocketAddress, path:String) extends ActorLink{

  override def accept(t: Message): Unit = {
    c.send(adr, RemoteMessage(path, t.message))
  }

  override def ask(v:Any) : CompletableFuture[Message] = {
    throw UnsupportedAskException(v)
  }
  
}
