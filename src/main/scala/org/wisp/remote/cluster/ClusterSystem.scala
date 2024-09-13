package org.wisp.remote.cluster

import org.wisp.remote.client.{ClientBinding, RemoteClient, SenderPath}
import org.wisp.remote.cluster.bus.{ClosedClusterConnection, FailedToCloseRemoteManager, FailedToCloseRemoteSystem}
import org.wisp.{ActorRuntime, ActorSystem}
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

  protected def createMassageBindMap(): ConcurrentMap[ObjectId, SenderPath] = new ConcurrentHashMap[ObjectId, SenderPath]()
  val massageBindMap: ConcurrentMap[ObjectId, SenderPath] = createMassageBindMap()

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

  override def forEach(action: BiConsumer[? >: ObjectId, ? >: RemoteContext]): Unit = {
    action.accept(this.id, this)
    connectedMap.forEach(action)
    remoteManager.forEach(action)
  }

  protected def createConnectedMap(): ConcurrentMap[ObjectId, ClusterConnection] = new ConcurrentHashMap[ObjectId, ClusterConnection]()
  private val connectedMap = createConnectedMap()

  override def createClientConnection(client: AsynchronousSocketChannel): ClientConnection = new ClusterConnection(this, client)

  def add(uuid:ObjectId, cc:ClusterConnection):Unit = {
    connectedMap.put(uuid, cc)
    val con = remoteManager.get(uuid)
    if(con != null) con.replaceBy(cc)

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
        }else publish(new ClosedClusterConnection(c, None))
      } else publish(new ClosedClusterConnection(c, Some(id)))
    }
    super.close(c)
  }

  override def shutdown(): CompletableFuture[Void] = {
    val f = new CompletableFuture[Void]

    // close outgoing
    remoteManager.disconnect().whenComplete{ (_, ex) =>
      if (ex != null) publish(new FailedToCloseRemoteManager(ClusterSystem.this, ex))

      // close ingoing
      super.shutdown().whenComplete { (_, exc) =>
        if (exc != null) publish(new FailedToCloseRemoteSystem(ClusterSystem.this, exc))

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
    ClientBinding.close(massageBindMap)
    remoteManager.close()
    super.close()
  }

}
