package org.wisp.udp

import java.net.SocketAddress
import java.nio.channels.DatagramChannel

class UdpRouter(address: SocketAddress) extends AutoCloseable{

  val channel:DatagramChannel = createDatagramChannel(address)

  protected def createDatagramChannel(address: SocketAddress):DatagramChannel = {
    DatagramChannel.open().bind(address)
  }



  override def close(): Unit = {
    channel.close()
  }

}
