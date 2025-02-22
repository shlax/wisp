package org.wisp.remote

import org.wisp.exceptions.ExceptionHandler
import org.wisp.{ActorRef, Message}

import java.net.SocketAddress

class RemoteRef(router: UdpClient, address:SocketAddress, path:String, exceptionHandler: ExceptionHandler) extends ActorRef(exceptionHandler){

  override def accept(t: Message): Unit = {
    router.send(address, RemoteMessage(path, t.message))
  }

}
