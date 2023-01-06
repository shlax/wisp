package org.qwActor.remote.cluster

import org.qwActor.{ActorMessage, ActorRef, logger}
import org.qwActor.remote.ObjectId
import org.qwActor.remote.client.{AskActorRef, RemoteClient}
import org.qwActor.remote.codec.{RemoteMessage, RemoteResponse}

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.{Callable, ConcurrentHashMap, ConcurrentMap}

class ClusterClient(system: ClusterSystem, manager: RemoteManager) extends RemoteClient {

  override protected def createObjectIdFactory(): Callable[ObjectId] = system.objectIdFactory

  override protected def getChannelGroup(): AsynchronousChannelGroup = system.channelGroup
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
