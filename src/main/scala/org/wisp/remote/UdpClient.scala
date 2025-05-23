package org.wisp.remote

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import org.wisp.closeable.*

class UdpClient(address: Option[SocketAddress] = None) extends AutoCloseable {

  protected val channel: DatagramChannel = createDatagramChannel(address)
  protected def createDatagramChannel(adr: Option[SocketAddress]): DatagramChannel = {
    val dc = DatagramChannel.open()
    for(a <- adr) dc.bind(a)
    dc
  }

  protected def write(m: RemoteMessage): Array[Byte] = {
    val bOut = new ByteArrayOutputStream()
    new ObjectOutputStream(bOut) | { out =>
      out.writeUTF(m.path)
      out.writeObject(m.value)
    }
    bOut.toByteArray
  }

  def send(adr: SocketAddress, m: RemoteMessage): Unit = {
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
