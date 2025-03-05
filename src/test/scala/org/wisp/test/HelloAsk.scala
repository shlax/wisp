package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorLink, ActorSystem, Inbox}
import java.util.concurrent.CountDownLatch
import org.wisp.test.testSystem.*

class HelloAsk {

  @Test
  def test():Unit = {
    val cd = new CountDownLatch(1)

    class HelloActor(in: Inbox) extends Actor(in) {
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a => from << "Hello " + a
      }
    }

    ActorSystem() || { sys =>

      val hello = sys.create(HelloActor(_))
      hello.ask("world").future.onComplete { e =>
        println(e.get.value)
        cd.countDown()
      }

      cd.await()
    }

  }

}
