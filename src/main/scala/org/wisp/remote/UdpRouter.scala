package org.wisp.remote

import org.wisp.{ActorRef, Message}
import org.wisp.using.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, ConcurrentMap, Executor}
import scala.annotation.targetName

class UdpRouter(address: SocketAddress, capacity:Int, executor: Executor) extends Runnable, AutoCloseable{

  val channel:DatagramChannel = createDatagramChannel(address)

  protected def createDatagramChannel(address: SocketAddress):DatagramChannel = {
    DatagramChannel.open().bind(address)
  }

  val bindMap: ConcurrentMap[String, ActorRef] = createBindMap()

  protected def createBindMap():ConcurrentMap[String, ActorRef] = {
    ConcurrentHashMap[String, ActorRef]()
  }

  def register(path:String, ref:ActorRef) : Option[ActorRef] = {
    Option(bindMap.put(path, ref))
  }

  val closed:AtomicBoolean = new AtomicBoolean(false)

  executor.execute(this)

  override def run(): Unit = {
    val buff = ByteBuffer.allocateDirect(capacity)
    while(!closed.get()){
      val adr = channel.receive(buff)
      buff.flip()
      val data = new Array[Byte](buff.remaining())
      buff.get(data)
      execute(adr, data)
      buff.clear()
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
      throw new IllegalStateException("not found " + rm.path)
    }

    ref.accept( Message( new ActorRef(ref.system){
        override def accept(t: Message): Unit = {
          t.message match {
            case m : RemoteMessage =>
              send(adr, m)
          }
        }
        //@targetName("ask")
        override def ask(v: Any): CompletableFuture[Message] = {
          throw new UnsupportedOperationException("ask pattern is not supported for remote")
        }
      }, rm.message) )
  }

  def write(m: RemoteMessage): Array[Byte] = {
    val bOut = new ByteArrayOutputStream()
    new ObjectOutputStream(bOut) | { out =>
      out.writeUTF(m.path)
      out.writeObject(m.message)
    }
    bOut.toByteArray
  }

  def send(adr: SocketAddress, m:RemoteMessage):Unit = {
    val buff = write(m)
    val r = channel.send(ByteBuffer.wrap(buff), adr)
    if(r != buff.length){
      throw new RuntimeException("message to long "+r+" "+buff.length)
    }
  }

  override def close(): Unit = {
    closed.set(true)
    channel.close()
  }

}
