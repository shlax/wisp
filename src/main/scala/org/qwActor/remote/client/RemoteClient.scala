package org.qwActor.remote.client

import org.qwActor.logger
import org.qwActor.{ActorMessage, ActorRef}
import org.qwActor.remote.codec.{ByteArrayDecoder, ByteArrayEncoder, Decoder, Disconnect, Encoder, RemoteMessage, RemoteResponse}
import org.qwActor.remote.{AbstractConnection, Attachment, ChannelGroup, ObjectId, ObjectIdFactory, RemoteContext, RemoteSystem}

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousChannelGroup, AsynchronousCloseException, AsynchronousSocketChannel, CompletionHandler}
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.{Queue, UUID}
import java.util.concurrent.{Callable, CompletableFuture, ConcurrentHashMap, ConcurrentLinkedQueue, ConcurrentMap, ExecutorService, Executors, ThreadFactory}

class RemoteClient extends AbstractConnection, ClientBinding, ChannelGroup, ObjectIdFactory {

  override val objectIdFactory:Callable[ObjectId] = createObjectIdFactory()

  override protected val channelGroup: AsynchronousChannelGroup= getChannelGroup()
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
        if(logger.isErrorEnabled) logger.error("socket channel accept failed: " + exc.getMessage, exc)
        connected.completeExceptionally(exc)
      }
    })
    connected
  }

  override def close(): Unit = {
    super[AbstractConnection].close()
    super[ChannelGroup].close()
    super[ClientBinding].close()
    connected.completeExceptionally(new AsynchronousCloseException)
  }

}
