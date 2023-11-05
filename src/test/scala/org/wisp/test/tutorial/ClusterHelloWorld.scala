package org.miniActor.test.tutorial

import org.miniActor.remote.RemoteSystem
import org.miniActor.remote.cluster.ClusterSystem
import org.miniActor.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import java.net.InetSocketAddress
import scala.util.Using

class ClusterHelloWorld extends AnyFunSuite{

  class HelloActor(context: ActorContext) extends Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case a =>
        println("received " + a)
        sender << "hello " + a
    }
  }

  test("clusterHelloWorld"){

    Using(ClusterSystem()) { system1 =>
      system1.create( new HelloActor(_) ).bind("hello")
      system1.bind(new InetSocketAddress(4321))

      Using(ClusterSystem()) { system2 =>
        system2.create( new HelloActor(_) ).bind("hello")
        system2.bind(new InetSocketAddress(4322))

        // connect and wait
        system2.addNode(new InetSocketAddress("127.0.0.1", 4321)).get()

        system2.forEach{ (id, context) =>
          println("" + id + " " + context.get("hello").ask("world").get().value)
        }

        system2.shutdown().get()

      }.get

      system1.shutdown().get()

    }.get

  }

}
