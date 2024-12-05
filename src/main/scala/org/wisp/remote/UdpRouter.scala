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
      execute(address, data)
      buff.clear()
    }
  }

  def execute(address: SocketAddress, data:Array[Byte]):Unit = {
    executor.execute{ () =>
      process(address, data)
    }
  }

  def process(address: SocketAddress, data: Array[Byte]): Unit = {
    new ObjectInputStream(new ByteArrayInputStream(data)) | { in =>
      val key = in.readUTF()
      val ref = bindMap.get(key)
      if(ref == null){
        throw new IllegalStateException("not found "+key)
      }
      val m = in.readObject()
      ref.accept( Message( new ActorRef(ref.system){
          override def accept(t: Message): Unit = {
            t.message match {
              case RemoteMessage(path, v) =>
                send(address, path, v)
            }
          }
          @targetName("ask")
          override def ?(v: Any): CompletableFuture[Message] = {
            throw new UnsupportedOperationException("ask pattern is not supported for remote")
          }
        }, m) )
    }
  }

  def send(address: SocketAddress, m:RemoteMessage):Unit = {
    send(address, m.path, m.message)
  }

  def send(address: SocketAddress, path:String, m:Any):Unit = {
    val bOut = new ByteArrayOutputStream(capacity)
    new ObjectOutputStream(bOut)|{ out =>
      out.writeUTF(path)
      out.writeObject(m)
    }
    val buff = bOut.toByteArray
    val r = channel.send(ByteBuffer.wrap(buff), address)
    if(r != buff.length){
      throw new RuntimeException("message to long "+r+" "+buff.length)
    }
  }

  override def close(): Unit = {
    closed.set(true)
    channel.close()
  }

}
