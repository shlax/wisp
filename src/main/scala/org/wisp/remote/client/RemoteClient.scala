package org.wisp.remote.client

import org.wisp.bus.{EventBus, JfrEventBus}
import org.wisp.remote.client.bus.FailedRemoteClientConnect
import org.wisp.remote.{AbstractConnection, ChannelGroup, ObjectId, ObjectIdFactory}

import java.net.InetSocketAddress
import java.nio.channels.{AsynchronousChannelGroup, AsynchronousCloseException, AsynchronousSocketChannel, CompletionHandler}
import java.util.concurrent.{Callable, CompletableFuture, ConcurrentMap}

class RemoteClient(bus:EventBus = new JfrEventBus) extends AbstractConnection(bus), ClientBinding, ChannelGroup, ObjectIdFactory {

  override val objectIdFactory:Callable[ObjectId] = createObjectIdFactory()

  override protected val channelGroup: AsynchronousChannelGroup = createChannelGroup()
  override protected val chanel: AsynchronousSocketChannel = AsynchronousSocketChannel.open(channelGroup)

  protected val connected = new CompletableFuture[ObjectId]()

  override def onConnected(uuid:ObjectId):Unit = {
    connected.complete(uuid)
  }

  override val bindMap: ConcurrentMap[ObjectId, SenderPath] = createBindMap()
  override def newBindId(): ObjectId = newObjectId()

  override protected def process: PartialFunction[Any, Unit] = ClientBinding.process(this)

  def connect(address:InetSocketAddress): CompletableFuture[ObjectId] = {
    chanel.connect(address, null, new CompletionHandler[Void, Void]{
      override def completed(result: Void, attachment: Void): Unit = {
        startReading()
      }

      override def failed(exc: Throwable, attachment: Void): Unit = {
        publish(new FailedRemoteClientConnect(RemoteClient.this, exc))
        connected.completeExceptionally(exc)
      }
    })
    connected
  }

  override def close(): Unit = {
    clearClientBinding()

    super[AbstractConnection].close()
    shutdownChannelGroup()

    connected.completeExceptionally(new AsynchronousCloseException)
  }

}
