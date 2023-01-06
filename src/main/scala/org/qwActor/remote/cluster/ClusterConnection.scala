package org.qwActor.remote.cluster

import org.qwActor.remote.client.{AskActorRef, ClientBinding, SenderPath}
import org.qwActor.remote.codec.{RemoteMessage, RemoteResponse}
import org.qwActor.{ActorMessage, ActorRef, AskMessage, logger}
import org.qwActor.remote.{ClientConnection, ObjectId, RemoteContext, RemoteSystem}

import java.nio.channels.{AsynchronousCloseException, AsynchronousSocketChannel}
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, ConcurrentMap}


class ClusterConnection(system:ClusterSystem, chanel: AsynchronousSocketChannel) extends ClientConnection(system, chanel) with ClientBinding{

  val connected = new CompletableFuture[ObjectId]()

  override def onConnected(uuid: ObjectId): Unit = {
    system.add(uuid, this)
    connected.complete(uuid)
  }

  override val bindMap: ConcurrentMap[ObjectId, SenderPath] = createBindMap()
  override def newBindId(): ObjectId = system.newObjectId()

  override def process: PartialFunction[Any, Unit] = super.process.orElse(ClientBinding.process(this))

  override def close(): Unit = {
    super[ClientConnection].close()
    super[ClientBinding].close()
    connected.completeExceptionally(new AsynchronousCloseException)
  }
}
