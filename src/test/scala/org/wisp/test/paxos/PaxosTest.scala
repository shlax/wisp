package org.wisp.test.paxos

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.remote.{RemoteLink, UdpClient, UdpRouter}
import org.wisp.serializer.{*, given}

import java.io.{DataInput, DataOutput}
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import scala.concurrent.ExecutionContext
import scala.util.Random

class PaxosTest {

  given ReadWrite[PaxosMessage] = new ReadWrite[PaxosMessage] {

    override def read(dataInput: DataInput): PaxosMessage = {
      val id = dataInput.readInt()
      id match {
        case 1 => readFrom[Prepare](dataInput)
        case 2 => readFrom[Promise](dataInput)
        case 3 => readFrom[Accept](dataInput)
        case 4 => readFrom[Accepted](dataInput)
        case 5 => readFrom[Ignore](dataInput)
        case 6 => readFrom[TryRun](dataInput)
      }
    }

    override def write(value: PaxosMessage, dataOutput: DataOutput): Unit = {
      value match {
        case m:Prepare => dataOutput.writeInt(1); m.writeTo(dataOutput)
        case m:Promise => dataOutput.writeInt(2); m.writeTo(dataOutput)
        case m:Accept => dataOutput.writeInt(3); m.writeTo(dataOutput)
        case m:Accepted => dataOutput.writeInt(4); m.writeTo(dataOutput)
        case m:Ignore => dataOutput.writeInt(5); m.writeTo(dataOutput)
        case m:TryRun => dataOutput.writeInt(6); m.writeTo(dataOutput)
      }
    }

  }

  def udpRouter(as:ActorSystem, id:Int): UdpRouter[String, PaxosMessage] = {
    given ExecutionContext = as
    val adr = InetSocketAddress("localhost", 9840 + id)
    UdpRouter[String, PaxosMessage](adr, 2024)
  }

  def createAcceptor(id:Int): (ActorSystem, UdpRouter[String, PaxosMessage]) = {
    val actorSystem = new ActorSystem
    val router = udpRouter(actorSystem, id)
    val acceptor = actorSystem.create(a => new Acceptor(id, a))
    router.register("paxos", acceptor)
    (actorSystem, router)
  }

  def createProposer(nodeId: Int, as: ActorSystem, acceptorsIds: List[Int], value: String, learner: CompletableFuture[String]): (Proposer, UdpClient[PaxosMessage]) = {
    given ExecutionContext = as
    val udpClient = UdpClient()
    val acceptors = acceptorsIds.map{ id =>
      val adr = InetSocketAddress("localhost", 9840 + id)
      RemoteLink[PaxosMessage](udpClient, adr)
    }
    val res = as.create(c => new Proposer(nodeId, value, acceptors, learner, c))
    (res, udpClient)
  }

  @Test
  def test() :Unit = {
    val learner = new CompletableFuture[String]

    val n1 = createAcceptor(1)
    val n2 = createAcceptor(2)
    val n3 = createAcceptor(3)

    val ids = List(1, 2, 3)

    val p1 = createProposer(1, n1._1, Random.shuffle(ids), "cat", learner)
    val p2 = createProposer(2, n2._1, Random.shuffle(ids), "dog", learner)
    val p3 = createProposer(3, n3._1, Random.shuffle(ids), "mouse", learner)

    p1._1 << TryRun(None)
    p2._1 << TryRun(None)
    p3._1 << TryRun(None)

    val res = learner.get()
    println(res)

    p1._2.close()
    p2._2.close()
    p3._2.close()

    n1._1.close()
    n2._1.close()
    n3._1.close()
  }

}
