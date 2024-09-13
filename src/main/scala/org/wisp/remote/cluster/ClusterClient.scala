package org.wisp.remote.cluster

import org.wisp.remote.ObjectId
import org.wisp.remote.client.{AskActorRef, RemoteClient, SenderPath}
import org.wisp.remote.cluster.bus.ClosedUnconnected

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.{Callable, ConcurrentMap}

class ClusterClient(system: ClusterSystem, manager: RemoteManager) extends RemoteClient(system) {

  override protected def createBindMap(): ConcurrentMap[ObjectId, SenderPath] = system.massageBindMap
  override protected def createObjectIdFactory(): Callable[ObjectId] = system.objectIdFactory
  override protected def createChannelGroup(): AsynchronousChannelGroup = system.channelGroup

  /** don't clear ClusterSystem massageBindMap */
  override def clearClientBinding(): Unit = {}

  /** don't clear ClusterSystem ChannelGroup */
  override protected def shutdownChannelGroup(): Unit = {}

  override protected def process: PartialFunction[Any, Unit] = super.process.orElse(AskActorRef.process(system, this))

  protected var replacedBy:Option[ClusterConnection] = None

  override protected def doSend(msg: Any): Unit = {
    if (replacedBy.isDefined) {
      replacedBy.get.send(msg)
    } else {
      super.doSend(msg)
    }
  }

  def  replaceBy(cc: ClusterConnection): Unit = {
    lock.lock()
    try {
      if (replacedBy.isDefined) {
        throw new IllegalStateException("already disconnected by " + replacedBy.get)
      }
      replacedBy = Some(cc)
    } finally {
      lock.unlock()
    }
  }

  override def close(): Unit = {
    val uid = connected.getNow(null)
    if (uid != null) manager.remove(uid)
    else publish(new ClosedUnconnected(this))

    super.close()
  }

  override def onConnected(uuid: ObjectId): Unit = {
    super.onConnected(uuid)
    send(system.id)
  }

}
