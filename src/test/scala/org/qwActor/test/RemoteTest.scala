package org.qwActor.test

import org.qwActor.remote.RemoteSystem
import org.qwActor.remote.client.RemoteClient
import org.qwActor.remote.cluster.ClusterSystem
import org.qwActor.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

class RemoteTest extends AnyFunSuite{

  class HelloActor(context: ActorContext) extends Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case i: Int =>
        println(""+Thread.currentThread()+" : "+"hello " + i)
      case s : String =>
        println(""+Thread.currentThread()+" : "+"hello "+s)
        sender << "reply "+s
    }
  }

  test("hello"){
    val as = new ActorSystem
    val s = new RemoteSystem(as)
    s.create(new HelloActor(_)).bind("hello")
    s.bind(new InetSocketAddress(6453))

    println(""+Thread.currentThread()+" : "+s.id)

    val c = new RemoteClient()

    c.connect(new InetSocketAddress("127.0.0.1", 6453)).thenAccept { id =>
      println(""+Thread.currentThread()+" : "+id)
    }.get()

    val ref = c.get("hello")
    ref << 123

    ref.ask("world").thenAccept { v =>
      println("" + Thread.currentThread() + " : " + "done " + v.value)
    }.get()

    c.disconnect().thenAccept { _ =>
      println(""+Thread.currentThread()+" : close")
    }.get()

    Thread.sleep(1000)

    c.close()
    s.close()
    as.close()

    Thread.sleep(1000)
  }

  test("helloCluster") {
    val as = new ActorSystem
    val s = new ClusterSystem(as)
    s.create(new HelloActor(_)).bind("hello")
    s.bind(new InetSocketAddress(6453))

    println("" + Thread.currentThread() + " : " + s.id)

    val c = new RemoteClient()

    c.connect(new InetSocketAddress("127.0.0.1", 6453)).thenAccept { id =>
      println("" + Thread.currentThread() + " : " + id)
    }.get()

    val ref = c.get("hello")
    ref << 123

    ref.ask("world").thenAccept { v =>
      println("" + Thread.currentThread() + " : " + "done " + v.value)
    }.get()

    c.disconnect().thenAccept { _ =>
      println("" + Thread.currentThread() + " : close")
    }.get()

    Thread.sleep(1000)

    c.close()
    s.close()
    as.close()

    Thread.sleep(1000)
  }

}
