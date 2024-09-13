package org.wisp.remote

import org.wisp.bus.Event
import org.wisp.remote.bus.{ClosedConnectedClient, ClosingRemoved, FailedCloseRemoteSystem, FailedRemoteSystem}
import org.wisp.{Actor, ActorContext, ActorRef, ActorRuntime, ActorSystem}

import java.net.InetSocketAddress
import java.nio.channels.{AsynchronousChannelGroup, AsynchronousCloseException, AsynchronousServerSocketChannel, AsynchronousSocketChannel, CompletionHandler}
import java.util.concurrent.{Callable, CompletableFuture, ConcurrentHashMap, ConcurrentMap}
import scala.util.control.NonFatal

object RemoteSystem{

  def apply():RemoteSystem = {
    val context = ActorSystem()
    new RemoteSystem(context){
      override def close(): Unit = {
        super.close()
        context.close()
      }
    }
  }

}

class RemoteSystem(context:ActorRuntime) extends RemoteActorRuntime, CompletionHandler[AsynchronousSocketChannel,Void], ChannelGroup, ObjectIdFactory, AutoCloseable {
  override def execute(command: Runnable): Unit = context.execute(command)
  override def publish(event: Event): Unit = context.publish(event)

  override val objectIdFactory:Callable[ObjectId] = createObjectIdFactory()
  val id: ObjectId = objectIdFactory.call()

  override val channelGroup: AsynchronousChannelGroup = createChannelGroup()
  private val serverChannel = AsynchronousServerSocketChannel.open(channelGroup)

  def bind(adr: InetSocketAddress): Unit = {
    serverChannel.bind(adr)
    serverChannel.accept(null, this)
  }

  def createClientConnection(client: AsynchronousSocketChannel): ClientConnection = new ClientConnection(this, client)

  protected def createConnectedSet() : ConcurrentMap[ClientConnection, ClientConnection] = new ConcurrentHashMap[ClientConnection, ClientConnection]()
  val connected: ConcurrentMap[ClientConnection, ClientConnection] = createConnectedSet()

  def close(c:ClientConnection):Unit = {
    val v = connected.remove(c)
    if(v == null) publish(new ClosingRemoved(v))
  }

  override def completed(client: AsynchronousSocketChannel, attachment: Void): Unit = {
    serverChannel.accept(null, this)
    add(createClientConnection(client))
  }

  def add(c:ClientConnection):Unit = {
    connected.put(c, c)
    c.startReading()
  }

  protected def handleAsynchronousCloseException(e:AsynchronousCloseException):Unit = { /* ignore */ }

  override def failed(exc: Throwable, attachment: Void): Unit = {
    exc match {
      case e : AsynchronousCloseException =>
        handleAsynchronousCloseException(e)
      case _ =>
        publish(new FailedRemoteSystem(this, exc))
    }
  }

  protected def createBindMap(): ConcurrentMap[Any, ActorRef] = new ConcurrentHashMap[Any, ActorRef]()
  val bindMap: ConcurrentMap[Any, ActorRef] = createBindMap()

  override def create(fn: ActorContext => Actor): RemoteRef = {
    val ref = context.create(fn)
    new RemoteRef(ref) {
      override def bind(path: Any): this.type = {
        bindMap.put(path, ref)
        this
      }
    }
  }

  override def get(path: Any): ActorRef = {
    val r = bindMap.get(path)
    if(r == null) throw new RuntimeException(""+path+" not found")
    r
  }

  override def close(path: Any): Option[ActorRef] = {
    val r = bindMap.remove(path)
    Option(r)
  }

  def shutdown(): CompletableFuture[Void] = {
    AbstractConnection.disconnect(connected)
  }

  override def close(): Unit = {
    connected.forEach{ (k, _) =>

      try {
        k.close()
        publish(new ClosedConnectedClient(k, None))
      }catch {
        case NonFatal(exc) =>
          publish(new ClosedConnectedClient(k, Some(exc)))
      }
    }

    try {
      serverChannel.close()
    }catch { case NonFatal(exc) =>
      publish(new FailedCloseRemoteSystem(this, exc))
    }

    shutdownChannelGroup()
  }
}
