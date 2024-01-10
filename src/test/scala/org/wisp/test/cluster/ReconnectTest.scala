package org.wisp.test.cluster

import org.scalatest.funsuite.AnyFunSuite
import org.wisp.remote.cluster.ClusterSystem
import org.wisp.{Actor, ActorContext, ActorRef}
import org.wisp.timer.Timer

import java.net.InetSocketAddress
import scala.concurrent.duration.*

class ReconnectTest extends AnyFunSuite{

  val timer = Timer()

  class DelayActor(context: ActorContext, nm:String) extends Actor(context) {

    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case s:String =>
        timer.scheduleMessage(sender, nm+"->"+s, 2.second)
    }

  }

  test("reconnectTest") {
    val s1 = ClusterSystem()
    s1.create(new DelayActor(_, "s1")).bind("hello")
    s1.bind(new InetSocketAddress(64531))

    val s2 = ClusterSystem()
    s2.create(new DelayActor(_, "s2")).bind("hello")
    s2.bind(new InetSocketAddress(64532))

    s1.addNode(new InetSocketAddress("127.0.0.1",64532)).get()
    Thread.sleep(1000)

    println("sending")

    s1.forEach{ (id, ctx) =>
      ctx.get("hello").ask("s1").thenAccept{ m =>
        println(m.value)
      }
    }

    s2.forEach { (id, ctx) =>
      ctx.get("hello").ask("s2").thenAccept{ m =>
        println(m.value)
      }
    }

    Thread.sleep(1000)

    // reverse connection
    s2.addNode(new InetSocketAddress("127.0.0.1",64531)).get()

    Thread.sleep(5000)

    s1.shutdown().get()
    s2.shutdown().get()

    s1.close()
    s2.close()

  }

}
