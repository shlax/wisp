package org.wisp.remote

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import org.wisp.serializer.ReadWrite
import org.wisp.utils.unsignedIntToBytes

import java.util.zip.CRC32C

/**
 * A UDP client for sending messages of type T over a datagram channel.
 *
 * This client serializes messages using the provided [[ReadWrite]] instance, adds CRC32C checksums
 * for data integrity verification, and sends them to remote addresses via UDP.
 *
 * @param address optional socket address to bind the datagram channel to
 */
class UdpClient[T](address: Option[SocketAddress] = None)(using ReadWrite[T]) extends AutoCloseable {

  protected val channel: DatagramChannel = createDatagramChannel(address)
  protected def createDatagramChannel(adr: Option[SocketAddress]): DatagramChannel = {
    val dc = DatagramChannel.open()
    for(a <- adr) dc.bind(a)
    dc
  }

  /**
   * Serializes a message and prepends a CRC32C checksum.
   *
   * The resulting byte array contains:
   * - Bytes 0-3: CRC32C checksum (4 bytes)
   * - Bytes 4+: Serialized message data
   *
   * @param message the message to serialize
   * @return byte array containing checksum and serialized message
   */
  protected def write(message: T): Array[Byte] = {
    val buff = message.toBytes()

    val crc = new CRC32C()
    crc.update(buff)
    val sum = crc.getValue

    val result = new Array[Byte](buff.length + 4)
    System.arraycopy(buff, 0, result, 4, buff.length)

    val hex = unsignedIntToBytes(sum)
    System.arraycopy(hex, 0, result, 0, 4)

    result
  }

  /**
   * Sends a message to the specified address.
   *
   * The message is serialized with a CRC32C checksum and sent via UDP.
   *
   * @param destination the destination socket address
   * @param message   the message to send
   */
  def send(destination: SocketAddress, message: T): Unit = {
    val buff = write(message)
    val r = channel.send(ByteBuffer.wrap(buff), destination)
    if (r != buff.length) {
      throw new RuntimeException("message to long " + r + " " + buff.length)
    }
  }

  /**
   * Closes the underlying datagram channel.
   */
  override def close(): Unit = {
    channel.close()
  }

}
