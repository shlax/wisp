package org.wisp.remote

import org.wisp.remote.exceptions.RemoteAskException
import org.wisp.{ActorLink, Message}
import org.wisp.io.{ReadWrite, extensions}

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import scala.concurrent.{ExecutionContext, Future}

class UdpRouter[K, M <: RemoteMessage[K] ](address: SocketAddress, capacity:Int)(using executor: ExecutionContext, rw:ReadWrite[M]) extends UdpClient(Some(address)), Runnable{

  protected val bindMap: ConcurrentMap[K, ActorLink] = createBindMap()

  protected def createBindMap():ConcurrentMap[K, ActorLink] = {
    ConcurrentHashMap[K, ActorLink]()
  }

  def register(path:K, ref:ActorLink) : Option[ActorLink] = {
    Option(bindMap.put(path, ref))
  }

  def remove(path: String): Option[ActorLink] = {
    Option(bindMap.remove(path))
  }

  protected val closed:AtomicBoolean = new AtomicBoolean(false)

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

  protected def execute(adr: SocketAddress, data:Array[Byte]):Unit = {
    executor.execute{ () =>
      process(adr, data)
    }
  }

  protected def read(data: Array[Byte]):M = {
    extensions.fromBytes[M](data)
  }

  protected def process(adr: SocketAddress, data: Array[Byte]): Unit = {
    val rm = read(data)

    val ref = bindMap.get(rm.path)
    if (ref == null) {
      throw new IllegalStateException("not found: " + rm.path)
    }

    ref.accept( Message( new ActorLink{
        override def accept(t: Message): Unit = {
          t.value match {
            case m : RemoteMessage[?] =>
              send(adr, m.asInstanceOf[M])
          }
        }

        override def call(v: Any): Future[Message] = {
          throw RemoteAskException(v)
        }
      }, rm) )
  }

  override def close(): Unit = {
    closed.set(true)
    super.close()
  }

}
