package org.wisp.remote

import org.wisp.remote.exceptions.RemoteAskException
import org.wisp.{ActorLink, Message}

import java.net.SocketAddress
import scala.concurrent.Future

class RemoteLink[T](c: UdpClient[T], adr:SocketAddress) extends ActorLink[T]{

  override def apply(t: Message[T]): Unit = {
    t.process(RemoteLink.this.getClass) {
      c.send(adr, t.value)
    }
  }

  override def call[R](v:T) : Future[Message[R]] = {
    throw RemoteAskException(v)
  }
  
}
