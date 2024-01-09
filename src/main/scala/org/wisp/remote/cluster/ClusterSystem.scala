package org.wisp.remote.cluster

import org.wisp.remote.client.RemoteClient
import org.wisp.{ActorRuntime, ActorSystem, logger}
import org.wisp.remote.{ClientConnection, ObjectId, ObjectIdFactory, RemoteContext, RemoteSystem}

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, ConcurrentMap}
import java.util.function.BiConsumer

object ClusterSystem{

  def apply(listener: Option[ClusterEventListener] = None):ClusterSystem = {
    val context = ActorSystem()
    new ClusterSystem(context, listener){
      override def close(): Unit = {
        super.close()
        context.close()
      }
    }
  }

}

class ClusterSystem(context:ActorRuntime, val listener: Option[ClusterEventListener] = None) extends RemoteSystem(context), ClusterContext, RemoteContext, ObjectIdFactory {

  protected def createRemoteManager(): RemoteManager = new RemoteManager(this)

  private val remoteManager = createRemoteManager()

  def addNode(address:InetSocketAddress): CompletableFuture[ObjectId] = {
    remoteManager.connect(address)
  }

  override def get(id: ObjectId): RemoteContext = {
    if(id == this.id) this
    else {
      val rc = remoteManager.get(id)
      if (rc != null) rc
      else{
        val tmp = connectedMap.get(id)
        if(tmp == null) throw new RuntimeException("client " + id + " not found")
        tmp
      }
    }
  }

  override def forEach(action: BiConsumer[_ >: ObjectId, _ >: RemoteContext]): Unit = {
    action.accept(this.id, this)
    connectedMap.forEach(action)
    remoteManager.forEach(action)
  }

  protected def createConnectedMap(): ConcurrentMap[ObjectId, ClusterConnection] = new ConcurrentHashMap[ObjectId, ClusterConnection]()
  private val connectedMap = createConnectedMap()

  override def createClientConnection(client: AsynchronousSocketChannel): ClientConnection = new ClusterConnection(this, client)

  def add(uuid:ObjectId, cc:ClusterConnection):Unit = {
    connectedMap.put(uuid, cc)
    for(l <- listener) l.added(uuid, cc)
  }

  def added(id:ObjectId, c:RemoteClient):Unit = {
    val conn = connectedMap.get(id)
    if(conn != null) conn.replaceBy(c)
    for (l <- listener) l.added(id, c)
  }

  override def close(c: ClientConnection): Unit = {
    if(c.isInstanceOf[ClusterConnection]){
      val cc = c.asInstanceOf[ClusterConnection]
      val cf = cc.connected

      if (cf.isDone && !cf.isCompletedExceptionally) {
        val id = cf.getNow(null)
        if(id != null) {
          connectedMap.remove(id)
          for (l <- listener) l.closed(id, cc)
        }else logger.debug("closing ClusterConnection["+c+"] without id")
      } else logger.debug("closing not connected ClusterConnection["+c+"]")
    }
    super.close(c)
  }

  override def shutdown(): CompletableFuture[Void] = {
    val f = new CompletableFuture[Void]

    // close outgoing
    remoteManager.disconnect().whenComplete{ (_, ex) =>
      if (ex != null && logger.isErrorEnabled) logger.error("cluster manager close failed " + ex.getMessage, ex)

      // close ingoing
      super.shutdown().whenComplete { (_, exc) =>
        if (exc != null && logger.isErrorEnabled) logger.error("cluster client close failed " + exc.getMessage, exc)

        if(ex != null && exc != null) {
          exc.addSuppressed(ex)
          f.completeExceptionally(exc)
        }else if(ex != null){
          f.completeExceptionally(ex)
        }else if(exc != null){
          f.completeExceptionally(exc)
        }else{
          f.complete(null)
        }
      }
    }

    f
  }

  override def close(): Unit = {
    remoteManager.close()
    super.close()
  }

}
