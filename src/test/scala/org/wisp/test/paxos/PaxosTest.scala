package org.wisp.test.paxos

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.remote.{RemoteLink, UdpRouter}
import org.wisp.serializer.{ReadWrite, readFrom}

import java.io.{DataInput, DataOutput}
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class PaxosTest {

  given ReadWrite[PaxosMessage] = new ReadWrite[PaxosMessage] {

    override def read(dataInput: DataInput): PaxosMessage = {
      val id = dataInput.readByte()
      id match {
        case 1 => readFrom[Prepare](dataInput)
        case 2 => readFrom[Promise](dataInput)
        case 3 => readFrom[Accept](dataInput)
        case 4 => readFrom[Accepted](dataInput)
        case 5 => readFrom[Ignore](dataInput)
      }
    }

    override def write(value: PaxosMessage, dataOutput: DataOutput): Unit = {
      value match {
        case m:Prepare =>
          dataOutput.writeByte(1)
          summon[ReadWrite[Prepare]].write(m, dataOutput)
        case m:Promise =>
          dataOutput.writeByte(2)
          summon[ReadWrite[Promise]].write(m, dataOutput)
        case m:Accept =>
          dataOutput.writeByte(3)
          summon[ReadWrite[Accept]].write(m, dataOutput)
        case m:Accepted =>
          dataOutput.writeByte(4)
          summon[ReadWrite[Accepted]].write(m, dataOutput)
        case m:Ignore =>
          dataOutput.writeByte(5)
          summon[ReadWrite[Ignore]].write(m, dataOutput)
      }
    }

  }

  class Node(id:Int, ids:List[Int], value:String) extends AutoCloseable{
    private val actorSystem = new ActorSystem
    given ExecutionContext = actorSystem

    private val address = InetSocketAddress("localhost", 9840 + id)
    private val router = UdpRouter[String, PaxosMessage](address, 2024)

    private val links = ids.map { i =>
      val adr = InetSocketAddress("localhost", 9840 + i)
      (i, RemoteLink[PaxosMessage](router, adr))
    }.toMap

    private val acceptor = actorSystem.create(a => new Acceptor(id, nId => links(nId), a))
    router.register("acceptor", acceptor)

    val learner : CompletableFuture[String] = new CompletableFuture[String]

    val proposer: Proposer = actorSystem.create(c => new Proposer(id, value, links.values.toList, learner, c))
    router.register("proposer", proposer)

    private val f = router.start

    override def close(): Unit = {
      try{
        router.close()
      }finally {
        actorSystem.close()
      }
      Await.ready(f, 10.seconds)
    }
  }

  @Test
  def test() :Unit = {
    val ids = List(1, 2, 3)

    val n1 = Node(1, ids, "cat")
    val n2 = Node(2, ids, "dog")
    val n3 = Node(3, ids, "mouse")

    n1.proposer << TryRun(None)
    n2.proposer << TryRun(None)
    n3.proposer << TryRun(None)

    val res1 = n1.learner.get()
    val res2 = n2.learner.get()
    Assertions.assertEquals(res1, res2)
    val res3 = n3.learner.get()
    Assertions.assertEquals(res2, res3)

    println(res3)

    Thread.sleep(10 * 1000)

    n1.close()
    n2.close()
    n3.close()
  }

}
