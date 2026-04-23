package org.wisp.remote

import org.wisp.remote.exceptions.RemoteAskException
import org.wisp.{Link, Message}

import java.net.SocketAddress
import scala.concurrent.Future

class RemoteLink[-T, +R](c: UdpClient[T], adr:SocketAddress) extends Link[T, R]{

  override def apply(t: Message[T, R]): Unit = {
    t.process(RemoteLink.this.getClass) {
      c.send(adr, t.value)
    }
  }

  override def call(v:T) : Future[Message[R, T]] = {
    throw RemoteAskException(v)
  }
  
}
