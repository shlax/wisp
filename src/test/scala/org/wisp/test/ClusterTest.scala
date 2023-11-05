package org.wisp.test

import org.wisp.remote.{ObjectId, RemoteContext}
import org.wisp.remote.cluster.{ClusterEventListener, ClusterSystem}
import org.wisp.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

class ClusterTest extends AnyFunSuite{

  class HelloActor(context: ActorContext, pref:String) extends Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case List(i) =>
        println(pref + " - " + "hello " + i)
      case s: String =>
        println(pref + " : " + "hello " + s)
        sender << pref+" reply " + s
    }
  }

  test("hello"){
    val pl = new ClusterEventListener {
      override def added(uuid: ObjectId, rc: RemoteContext): Unit = {
        println("added "+uuid)
      }
      override def closed(uuid: ObjectId, rc: RemoteContext): Unit = {
        println("closed "+uuid)
      }
    }

    val idCf = new CompletableFuture[ObjectId]
    val l = new ClusterEventListener {
      override def added(uuid: ObjectId, rc: RemoteContext): Unit = {
        idCf.complete(uuid)
      }
    }

    val as1 = new ActorSystem
    val s1 = new ClusterSystem(as1, Some(pl))
    s1.create(new HelloActor(_, "s1")).bind("hello")
    s1.bind(new InetSocketAddress(64531))

    val as2 = new ActorSystem
    val s2 = new ClusterSystem(as2, Some(l.add(pl)) )
    s2.create(new HelloActor(_, "s2")).bind("hello")
    s2.bind(new InetSocketAddress(64532))

    val id2 = s1.addNode(new InetSocketAddress("127.0.0.1",64532)).get()
    val id1 = idCf.get()

    s1.forEach{ (k, _) => println(""+id1+" -> "+k) }
    s2.forEach{ (k, _) => println(""+id2+" -> "+k) }

    println()

    s1.get(id1).get("hello") << List("world 11")
    s1.get(id2).get("hello") << List("world 12")

    s2.get(id1).get("hello") << List("world 21")
    s2.get(id2).get("hello") << List("world 22")

    println( s1.get(id1).get("hello").ask("world ? 11").get().value )
    println( s1.get(id2).get("hello").ask("world ? 12").get().value )

    println( s2.get(id1).get("hello").ask("world ? 21").get().value )
    println( s2.get(id2).get("hello").ask("world ? 22").get().value )

    println()

    s1.shutdown().get()
    s2.shutdown().get()

    s1.close()
    s2.close()

    as1.close()
    as2.close()

  }

}
