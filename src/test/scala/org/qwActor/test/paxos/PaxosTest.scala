package org.qwActor.test.paxos

import org.qwActor.remote.{ObjectId, RemoteContext, RemoteRef}
import org.qwActor.remote.cluster.{ClusterEventListener, ClusterSystem}
import org.qwActor.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import scala.collection.mutable
import scala.util.control.NonFatal

class PaxosTest extends AnyFunSuite{

  def createAcceptor(id:Int, l:CompletableFuture[ObjectId]): (ActorSystem, ClusterSystem) = {
    val as = new ActorSystem
    val cs = new ClusterSystem(as, Some(new ClusterEventListener{
      override def added(uuid: ObjectId, rc: RemoteContext): Unit = {
        l.complete(uuid)
      }
    }))
    cs.bind(new InetSocketAddress(64530+id))
    cs.create(a => new Acceptor(a)).bind("acceptor")
    (as, cs)
  }

  def createProposer(nodeId: Int, s: ClusterSystem, acceptorsId: List[ObjectId], value: Any, learner: CompletableFuture[Any]): RemoteRef = {
    val acceptors = acceptorsId.map(i => s.get(i).get("acceptor"))
    s.create(c => new Proposer(nodeId, value, acceptors, learner, c))
  }

  test("paxos"){
    val learner = new CompletableFuture[Any]

    val connected12 = new CompletableFuture[ObjectId]
    val connected23 = new CompletableFuture[ObjectId]
    val connected31 = new CompletableFuture[ObjectId]

    val n1 = createAcceptor(1, connected12)
    val n2 = createAcceptor(2, connected23)
    val n3 = createAcceptor(3, connected31)

    n1._2.addNode(new InetSocketAddress("127.0.0.1", 64530+2)).get()
    n2._2.addNode(new InetSocketAddress("127.0.0.1", 64530+3)).get()
    n3._2.addNode(new InetSocketAddress("127.0.0.1", 64530+1)).get()

    val ids = List(connected12.get(), connected23.get(), connected31.get())

    val p1 = createProposer(1, n1._2, ids, "cat", learner)
    val p2 = createProposer(2, n2._2, ids, "dog", learner)
    val p3 = createProposer(3, n3._2, ids, "mouse", learner)

    p3 << TryRun(None)
    p1 << TryRun(None)
    p2 << TryRun(None)

    println(learner.get())

    // wait for messages to stop
     Thread.sleep(500)

    n1._2.shutdown().get()
    n2._2.shutdown().get()
    n3._2.shutdown().get()

    n1._2.close()
    n2._2.close()
    n3._2.close()

    n1._1.close()
    n2._1.close()
    n3._1.close()
  }

}
