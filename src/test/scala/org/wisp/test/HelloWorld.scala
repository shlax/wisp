package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorLink, ActorSystem, Inbox}
import org.wisp.test.testSystem.*
import java.util.concurrent.CountDownLatch

class HelloWorld {

  @Test
  def test(): Unit = {
    val cd = new CountDownLatch(1)

    class HelloActor(in: Inbox) extends Actor(in) {
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a =>
          println(a)
          cd.countDown()
      }
    }

    new ActorSystem() || { sys =>
      val hello = sys.create(HelloActor(_))
      hello << "Hello world"

      cd.await()
    }
  }

}
