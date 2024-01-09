package org.wisp.remote.cluster

import org.wisp.remote.client.{ClientBinding, RemoteClient, SenderPath}
import org.wisp.remote.{ClientConnection, ObjectId}

import java.nio.channels.{AsynchronousCloseException, AsynchronousSocketChannel}
import java.util.concurrent.{CompletableFuture, ConcurrentMap}


class ClusterConnection(system:ClusterSystem, chanel: AsynchronousSocketChannel) extends ClientConnection(system, chanel) with ClientBinding{

  val connected = new CompletableFuture[ObjectId]()

  override def onConnected(uuid: ObjectId): Unit = {
    system.add(uuid, this)
    connected.complete(uuid)
  }

  override val bindMap: ConcurrentMap[ObjectId, SenderPath] = createBindMap()
  override def newBindId(): ObjectId = system.newObjectId()

  override def process: PartialFunction[Any, Unit] = super.process.orElse(ClientBinding.process(this))

  @volatile
  private var remoteClient:Option[RemoteClient] = None

  override def send(msg: Any): Unit = {
    val rc = remoteClient
    if(rc.isDefined){
      rc.get.send(msg)
    }else {
      super.send(msg)
    }
  }

  def disconnect(c:RemoteClient): CompletableFuture[Void] = {
    remoteClient = Some(c)
    disconnect()
  }

  override def close(): Unit = {
    super[ClientConnection].close()
    super[ClientBinding].close()
    connected.completeExceptionally(new AsynchronousCloseException)
  }
}
