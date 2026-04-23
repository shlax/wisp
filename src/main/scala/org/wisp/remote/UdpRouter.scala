package org.wisp.remote

import org.wisp.remote.exceptions.RemoteAskException
import org.wisp.{Link, Message}
import org.wisp.serializer.*
import org.wisp.utils.bytesToUnsignedInt

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import java.util.zip.CRC32C
import scala.concurrent.{ExecutionContext, Future}


/**
 * UDP-based router that receives remote messages and dispatches them to registered actors.
 *
 * This class manages a UDP server that listens for incoming messages, validates them using CRC32C checksums,
 * deserializes them, and routes them to the appropriate actor based on the message path.
 *
 * @tparam K the type of the path key used to identify registered actors
 * @tparam M the type of remote messages
 * @param address  the socket address to bind the UDP server to
 * @param capacity the buffer capacity for receiving UDP packets (If capacity is less that bytes that are required to hold the datagram then the remainder of the datagram is silently discarded)
 * @param executor the execution context for asynchronous operations
 * @param read the ReadWrite serializer for message deserialization
 * @param write the ReadWrite serializer for message serialization
 */
class UdpRouter[K, M <: RemoteMessage[K], R](address: SocketAddress, capacity: Int)(using executor: ExecutionContext, read: ReadWrite[M], write: ReadWrite[R])
  extends UdpClient[R](Some(address)), Runnable {

  /**
   * Map that associates path keys with actor references for message routing.
   */
  protected val bindMap: ConcurrentMap[K, Link[M, R]] = createBindMap()

  protected def createBindMap(): ConcurrentMap[K, Link[M, R]] = {
    ConcurrentHashMap[K, Link[M, R]]()
  }

  /**
   * Registers an actor reference for a given path.
   *
   * @param path the path key to register
   * @param link  the actor reference to associate with the path
   * @return Some(previousActorLink) if a mapping already existed, None otherwise
   */
  def register(path: K, link: Link[M, R]): Option[Link[M, R]] = {
    Option(bindMap.put(path, link))
  }

  /**
   * Removes an actor registration for a given path.
   *
   * @param path the path to remove
   * @return Some(removedActorLink) if a mapping existed, None otherwise
   */
  def remove(path: K): Option[Link[M, R]] = {
    Option(bindMap.remove(path))
  }

  protected val closed: AtomicBoolean = new AtomicBoolean(false)

  /**
   * Starts the UDP router in a separate future.
   *
   * @return a Future that completes when the router stops
   */
  def start: Future[Unit] = Future {
    run()
  }

  /**
   * Main loop that receives UDP packets, validates them, and dispatches them for processing.
   * This method blocks until the router is closed.
   */
  override def run(): Unit = {
    val buff = ByteBuffer.allocateDirect(capacity)
    while(!closed.get()){
      val adr = try{
        Some(channel.receive(buff))
      }catch{
        case e: AsynchronousCloseException =>
          if(closed.get()) None else throw e
      }
      for(a <- adr) {
        buff.flip()
        val data = new Array[Byte](buff.remaining())
        buff.get(data)
        execute(a, data)
        buff.clear()
      }
    }
  }

  protected def execute(adr: SocketAddress, data: Array[Byte]): Unit = {
    executor.execute { () =>
      process(adr, data)
    }
  }

  /**
   * Deserializes and validates a message from raw bytes.
   *
   * @param data the raw message data including CRC32C checksum in first 4 bytes
   * @return the deserialized message
   * @throws RuntimeException if the CRC32C checksum validation fails
   */
  protected def read(data: Array[Byte]):M = {
    val crc = new CRC32C()
    crc.update(data, 4, data.length - 4)
    val sum = crc.getValue

    val hex = bytesToUnsignedInt(data)

    if(hex != sum){
      throw new RuntimeException("crc error " + hex + " != " + sum)
    }

    fromBytes[M](data, true, 4, data.length - 4)
  }

  /**
   * Processes a received message by deserializing it and routing it to the registered actor.
   *
   * Creates a reply-capable ActorLink that sends responses back to the original sender address.
   *
   * @param adr  the socket address of the sender
   * @param data the raw message data
   */
  protected def process(adr: SocketAddress, data: Array[Byte]): Unit = {
    val rm = read(data)

    val ref = bindMap.get(rm.path)
    if (ref == null) {
      throw new IllegalStateException("not found: " + rm.path)
    }

    ref.apply( Message[M, R]( rm, new Link[R, M]{
        override def apply(t: Message[R, M]): Unit = {
          t.process(UdpRouter.this.getClass) {
            send(adr, t.value)
          }
        }
        override def call(v:R) : Future[Message[M, R]] = {
          throw RemoteAskException(v)
        }
      }) )
  }

  /**
   * Closes the UDP router, stopping message reception and closing the underlying channel.
   */
  override def close(): Unit = {
    closed.set(true)
    super.close()
  }

}
