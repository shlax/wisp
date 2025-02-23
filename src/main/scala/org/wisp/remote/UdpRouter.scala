package org.wisp.remote

import org.wisp.remote.exceptions.UnsupportedAskException
import org.wisp.{ActorLink, Message}
import org.wisp.using.*

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, ConcurrentMap, Executor}

class UdpRouter(address: SocketAddress, capacity:Int, executor: Executor) extends UdpClient(address), Runnable{

  protected val bindMap: ConcurrentMap[String, ActorLink] = createBindMap()

  protected def createBindMap():ConcurrentMap[String, ActorLink] = {
    ConcurrentHashMap[String, ActorLink]()
  }

  def register(path:String, ref:ActorLink) : Option[ActorLink] = {
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

  protected def read(data: Array[Byte]):RemoteMessage = {
    new ObjectInputStream(new ByteArrayInputStream(data)) | { in =>
      RemoteMessage(in.readUTF(), in.readObject())
    }
  }

  protected def process(adr: SocketAddress, data: Array[Byte]): Unit = {
    val rm = read(data)

    val ref = bindMap.get(rm.path)
    if (ref == null) {
      throw new IllegalStateException("not found: " + rm.path)
    }

    ref.accept( Message( new ActorLink{
        override def accept(t: Message): Unit = {
          t.message match {
            case m : RemoteMessage =>
              send(adr, m)
          }
        }

        override def ask(v: Any): CompletableFuture[Message] = {
          throw UnsupportedAskException(v)
        }
      }, rm.message) )
  }

  override def close(): Unit = {
    closed.set(true)
    super.close()
  }

}
