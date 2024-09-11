package org.wisp.remote.cluster

import org.wisp.logger
import org.wisp.remote.{ObjectId, RemoteContext, AbstractConnection}
import org.wisp.remote.client.RemoteClient

import java.net.InetSocketAddress
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, ConcurrentMap}
import java.util.function.BiConsumer
import scala.util.control.NonFatal

class RemoteManager(system: ClusterSystem) extends ClusterContext, AutoCloseable{

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
    if(old != null && logger.isWarnEnabled) logger.warn("replacing node ["+id+"] "+old)

    system.added(id, c)
  }

  def remove(id: ObjectId): Unit = {
    if(id == null) throw new NullPointerException("id is null")

    val old = connectedMap.remove(id)
    if (old != null){
      for(l <- system.listener) l.closed(id, old)
    }else if(logger.isWarnEnabled){
      logger.warn("already removed "+id)
    }
  }

  def connect(address:InetSocketAddress):CompletableFuture[ObjectId] = {
    val c = createClient(address)
    c.connect(address).whenComplete{ (uuid, exc) =>
      if(exc != null){
        if(logger.isErrorEnabled) logger.error("cluster client connection failed "+exc.getMessage, exc)
        c.close()
      }else add(uuid, c)
    }
  }

  def disconnect(): CompletableFuture[Void] = {
    AbstractConnection.disconnect(connectedMap)
  }

  override def close(): Unit = {
    connectedMap.forEach { (k, v) =>
      if(logger.isWarnEnabled) logger.warn("closing connected client "+k)
      try{ v.close()
      }catch { case NonFatal(exc) =>
        if(logger.isErrorEnabled) logger.error("remote client close "+k+" failed " + exc.getMessage, exc)
      }
    }
  }

}
