package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorLink, ActorSystem, Inbox, Message}
import org.wisp.using.*

import java.util.concurrent.CountDownLatch
import scala.concurrent.ExecutionContextExecutorService

class HelloAsk {

  val cd = new CountDownLatch(1)

  class HelloActor(in:Inbox) extends Actor(in){
    override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
      case a => from << "Hello "+a
    }
  }

  @Test
  def test():Unit = {
    ActorSystem() |? { sys =>

      val hello = sys.create(HelloActor(_))
      hello.ask("world").future.onComplete { e =>
        println(e.get.value)
        cd.countDown()
      }

      cd.await()
    }
  }

}
