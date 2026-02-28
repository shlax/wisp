package org.wisp.remote

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import org.wisp.serializer.ReadWrite

import java.util.zip.CRC32C

class UdpClient[T](address: Option[SocketAddress] = None)(using rw:ReadWrite[T]) extends AutoCloseable {

  protected val channel: DatagramChannel = createDatagramChannel(address)
  protected def createDatagramChannel(adr: Option[SocketAddress]): DatagramChannel = {
    val dc = DatagramChannel.open()
    for(a <- adr) dc.bind(a)
    dc
  }

  protected def write(m: T): Array[Byte] = {
    val buff = m.toBytes

    val crc = new CRC32C()
    crc.update(buff)
    var sum = crc.getValue

    val result = new Array[Byte](buff.length + 4)
    System.arraycopy(buff, 0, result, 4, buff.length)

    var i = 3
    while (i >= 0) {
      result(i) = (sum & 0xFF).asInstanceOf[Byte]
      sum >>= 8
      i -= 1
    }

    result
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
