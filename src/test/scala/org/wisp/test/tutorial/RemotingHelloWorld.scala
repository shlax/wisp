package org.miniActor.test.tutorial

import org.miniActor.remote.RemoteSystem
import org.miniActor.remote.client.RemoteClient
import org.miniActor.remote.cluster.ClusterSystem
import org.miniActor.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import java.net.InetSocketAddress
import scala.util.Using

class RemotingHelloWorld extends AnyFunSuite{

  class HelloActor(context: ActorContext) extends Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case a =>
        println("received " + a)
        sender << "hello "+a
    }
  }

  test("helloWorld"){

    // server
    Using(RemoteSystem()) { system =>  // or ClusterSystem
      system.create(new HelloActor(_)).bind("hello")
      system.bind(new InetSocketAddress(4321))

      // client
      Using(new RemoteClient){ client =>
        // connect and wait
        client.connect(new InetSocketAddress("127.0.0.1", 4321)).get()

        // reference to: system.create(new HelloActor(_)).bind("hello")
        val helloRef = client.get("hello")
        println(helloRef.ask("world").get().value)

        //wait for disconnect
        client.disconnect().get()
      }.get

      Thread.sleep(1000)

      // shutdown / close
      system.shutdown().get()
    }.get

  }

}
