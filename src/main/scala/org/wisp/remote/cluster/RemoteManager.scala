package org.wisp.remote.cluster

import org.wisp.bus.Event
import org.wisp.remote.{AbstractConnection, ObjectId, RemoteContext}
import org.wisp.remote.cluster.bus.{ClosedConnectedClusterClient, FailedClusterClientConnection, ReplacingClusterClient}

import java.net.InetSocketAddress
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, ConcurrentMap}
import java.util.function.BiConsumer
import scala.util.control.NonFatal

class RemoteManager(system: ClusterSystem) extends ClusterContext, AutoCloseable{
  override def publish(event: Event): Unit = system.publish(event)

  protected def createConnectedMap(): ConcurrentMap[ObjectId, ClusterClient] = new ConcurrentHashMap[ObjectId, ClusterClient]()
  private val connectedMap = createConnectedMap()

  protected def createClient(address:InetSocketAddress): ClusterClient = new ClusterClient(system, this)

  override def get(id: ObjectId): ClusterClient = {
    if(id == null) throw new NullPointerException("id is null")
    connectedMap.get(id)
  }

  override def forEach(action: BiConsumer[? >: ObjectId, ? >: RemoteContext]):Unit = {
    connectedMap.forEach(action)
  }

  def add(id:ObjectId, c:ClusterClient):Unit = {
    if(id == null) throw new NullPointerException("id is null")

    val old = connectedMap.put(id, c)
    if(old != null) system.publish(new ReplacingClusterClient(id, old))

    system.added(id, c)
  }

  def remove(id: ObjectId): Boolean = {
    if(id == null) throw new NullPointerException("id is null")

    val old = connectedMap.remove(id)
    if (old != null){
      for(l <- system.listener) l.closed(id, old)
      true
    }else false
  }

  def connect(address:InetSocketAddress):CompletableFuture[ObjectId] = {
    val c = createClient(address)
    c.connect(address).whenComplete{ (uuid, exc) =>
      if(exc != null){
        try {
          c.close()
        }finally {
          publish(new FailedClusterClientConnection(c, exc))
        }
      }else add(uuid, c)
    }
  }

  def disconnect(): CompletableFuture[Void] = {
    AbstractConnection.disconnect(connectedMap)
  }

  override def close(): Unit = {
    connectedMap.forEach { (k, v) =>
      try{
        v.close()
        publish(new ClosedConnectedClusterClient(k, v, None))
      }catch { case NonFatal(exc) =>
        publish(new ClosedConnectedClusterClient(k, v, Some(exc)))
      }
    }
  }

}
