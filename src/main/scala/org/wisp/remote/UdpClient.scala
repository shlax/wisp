package org.wisp.remote

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import org.wisp.io.ReadWrite
import org.wisp.io.codec.toBytes

class UdpClient[T](address: Option[SocketAddress] = None)(using rw:ReadWrite[T]) extends AutoCloseable {

  protected val channel: DatagramChannel = createDatagramChannel(address)
  protected def createDatagramChannel(adr: Option[SocketAddress]): DatagramChannel = {
    val dc = DatagramChannel.open()
    for(a <- adr) dc.bind(a)
    dc
  }

  protected def write(m: T): Array[Byte] = {
    m.toBytes
  }

  def send(adr: SocketAddress, m: T): Unit = {
    val buff = write(m)
    val r = channel.send(ByteBuffer.wrap(buff), adr)
    if (r != buff.length) {
      throw new RuntimeException("message to long " + r + " " + buff.length)
    }
  }

  override def close(): Unit = {
    channel.close()
  }

}
