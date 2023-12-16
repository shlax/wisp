package org.wisp.remote.cluster.topology

import org.wisp.ActorSystem
import org.wisp.remote.ObjectId
import org.wisp.remote.cluster.{ClusterEventListener, ClusterSystem}

import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

object Cluster{

  def apply():Cluster = new Cluster()

}

class Cluster extends ClusterEventListener{

  protected def createClusterSystem():ClusterSystem = ClusterSystem(Some(this))
  val system:ClusterSystem = createClusterSystem()

  def bind(adr:InetSocketAddress):Unit = {
    system.bind(adr)
  }

  def connect(self:InetSocketAddress, nodes:Seq[InetSocketAddress]):List[CompletableFuture[(InetSocketAddress,ObjectId)]] = {
    val c = ConnectionBalancer.apply(nodes)
    for(i <- c if i.from == self) yield {
      val cf = new CompletableFuture[(InetSocketAddress,ObjectId)]()
      system.addNode(i.to).whenComplete{ (id, ex) =>
        if(ex != null) cf.completeExceptionally(new ConnectionException(i.to, ex))
        else cf.complete((i.to, id))
      }
      cf
    }
  }
  
}
