package org.wisp.test.remote

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorLink, ActorSystem}
import org.wisp.remote.{RemoteMessage, UdpClient, UdpRouter}
import org.wisp.using.*

import java.net.InetSocketAddress
import java.util.concurrent.{CountDownLatch, TimeUnit}

class HelloRemote {

  @Test
  def test(): Unit = {
    val cd = CountDownLatch(2)
    val adr = InetSocketAddress("localhost", 9845)

    using{ use =>
      val s = use(ActorSystem())

      val r = use(UdpRouter(adr, 2024, s))
      s.execute(r)

      r.register("echo", s.create(i => new Actor(i){
        override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
          case x:Any =>
            print(""+x+" ")
            cd.countDown()
        }
      }))

      val c = use(UdpClient())

      c.send(adr, RemoteMessage("echo", "Hello"))
      Thread.sleep(50)
      c.send(adr, RemoteMessage("echo", "world"))

      cd.await(3, TimeUnit.SECONDS)
      println()
    }
  }

}
