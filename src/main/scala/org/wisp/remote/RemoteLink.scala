package org.wisp.remote

import org.wisp.remote.exceptions.RemoteAskException
import org.wisp.{Link, Message}

import java.net.SocketAddress
import scala.concurrent.Future

/**
 * Link that sends messages over UDP.
 *
 * `ask` and `call` operations are not supported.
 */
class RemoteLink[-T, +R](client: UdpClient[T], address:SocketAddress) extends Link[T, R]{

  override def apply(t: Message[T, R]): Unit = {
    t.process(RemoteLink.this.getClass) {
      client.send(address, t.value)
    }
  }

  override def call(v:T) : Future[Message[R, T]] = {
    throw RemoteAskException(v)
  }
  
}
