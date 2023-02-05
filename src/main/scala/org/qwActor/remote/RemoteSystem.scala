package org.qwActor.remote

import org.qwActor.{Actor, ActorContext, ActorRef, ActorRuntime, ActorSystem, logger}
import org.slf4j.LoggerFactory

import java.net.InetSocketAddress
import java.nio.channels.{AsynchronousChannelGroup, AsynchronousCloseException, AsynchronousServerSocketChannel, AsynchronousSocketChannel, CompletionHandler}
import java.nio.channels.spi.AsynchronousChannelProvider
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Callable, CompletableFuture, ConcurrentHashMap, ConcurrentMap, ExecutorService, Executors, ForkJoinPool}
import scala.collection.mutable
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

class RemoteSystem(context:ActorRuntime) extends RemoteActorRuntime, CompletionHandler[AsynchronousSocketChannel,Void], ChannelGroup, ObjectIdFactory {

  override val objectIdFactory:Callable[ObjectId] = createObjectIdFactory()
  val id: ObjectId = objectIdFactory.call()

  override val channelGroup = getChannelGroup()
  private val serverChannel = AsynchronousServerSocketChannel.open(channelGroup)

  def bind(adr: InetSocketAddress): Unit = {
    serverChannel.bind(adr)
    serverChannel.accept(null, this)
  }

  def createClientConnection(client: AsynchronousSocketChannel): ClientConnection = new ClientConnection(this, client)

  protected def createConnectedSet() : ConcurrentMap[ClientConnection, ClientConnection] = new ConcurrentHashMap[ClientConnection, ClientConnection]()
  val connected = createConnectedSet()

  def close(c:ClientConnection):Unit = {
    val v = connected.remove(c)
    if(v == null && logger.isWarnEnabled) logger.warn("closing removed "+c)
  }

  override def completed(client: AsynchronousSocketChannel, attachment: Void): Unit = {
    serverChannel.accept(null, this)
    add(createClientConnection(client))
  }

  def add(c:ClientConnection):Unit = {
    connected.put(c, c)
    c.startReading()
  }

  override def failed(exc: Throwable, attachment: Void): Unit = {
    exc match {
      case _ : AsynchronousCloseException =>
        logger.debug("socket channel accept failed: " + exc.getMessage, exc)
      case _ =>
        if(logger.isErrorEnabled) logger.error("socket channel accept failed: " + exc.getMessage, exc)
    }
  }

  protected def createBindMap(): ConcurrentMap[Any, ActorRef] = new ConcurrentHashMap[Any, ActorRef]()
  private val bindMap = createBindMap()

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
      if(logger.isWarnEnabled) logger.warn("closing connected client "+k)
      try { k.close()
      }catch { case NonFatal(exc) =>
        if(logger.isErrorEnabled)logger.error("client channel close "+k+" failed " + exc.getMessage, exc) }
    }

    try { serverChannel.close()
    }catch { case NonFatal(exc) =>
      if(logger.isErrorEnabled) logger.error("socket channel close "+serverChannel+" failed " + exc.getMessage, exc)
    }

    super.close()
  }
}
