package org.wisp.remote

import org.wisp.exceptions.ExceptionHandler
import org.wisp.{ActorLink, Message}

import java.net.SocketAddress
import java.util.concurrent.CompletableFuture

class RemoteActorLink(c: UdpClient, adr:SocketAddress, path:String, eh: ExceptionHandler) extends ActorLink(eh){

  override def accept(t: Message): Unit = {
    c.send(adr, RemoteMessage(path, t.message))
  }

  override def ask(v:Any) : CompletableFuture[Message] = {
    throw new UnsupportedOperationException("ask pattern is not supported for remote")
  }
  
}
