package org.wisp.remote

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import org.wisp.serializer.ReadWrite

import java.util.HexFormat
import java.util.zip.CRC32C

class UdpClient[T](address: Option[SocketAddress] = None)(using rw:ReadWrite[T]) extends AutoCloseable {

  protected val channel: DatagramChannel = createDatagramChannel(address)
  protected def createDatagramChannel(adr: Option[SocketAddress]): DatagramChannel = {
    val dc = DatagramChannel.open()
    for(a <- adr) dc.bind(a)
    dc
  }

  protected def write(m: T): Array[Byte] = {
    val buff = m.toBytes()

    val crc = new CRC32C()
    crc.update(buff)
    var sum = crc.getValue.toHexString
    while(sum.length < 8) sum = "0"+sum

    val result = new Array[Byte](buff.length + 4)
    System.arraycopy(buff, 0, result, 4, buff.length)

    val hex = HexFormat.of().parseHex(sum)
    System.arraycopy(hex, 0, result, 4 - hex.length, hex.length)

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
