package org.miniActor.remote.cluster

import org.miniActor.logger
import org.miniActor.remote.ObjectId
import org.miniActor.remote.client.{AskActorRef, RemoteClient}

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Callable

class ClusterClient(system: ClusterSystem, manager: RemoteManager) extends RemoteClient {

  override protected def createObjectIdFactory(): Callable[ObjectId] = system.objectIdFactory

  override protected def createChannelGroup(): AsynchronousChannelGroup = system.channelGroup
  override protected def shutdownChannelGroup(): Boolean = false

  override protected def process: PartialFunction[Any, Unit] = super.process.orElse(AskActorRef.process(system, this))

  override def close(): Unit = {
    val uid = connected.getNow(null)
    if (uid != null) manager.remove(uid)
    else logger.debug("shutting down unconnected node " + this)

    super.close()
  }

  override def onConnected(uuid: ObjectId): Unit = {
    super.onConnected(uuid)
    send(system.id)
  }

}
