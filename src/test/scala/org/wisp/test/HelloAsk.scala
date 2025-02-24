package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorLink, ActorSystem, Inbox, Message}
import org.wisp.using.*

import java.util.concurrent.CountDownLatch

class HelloAsk {

  val cd = new CountDownLatch(1)

  class HelloActor(in:Inbox) extends Actor(in){
    override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
      case a => from << "Hello "+a
    }
  }

  @Test
  def test():Unit = {
    ActorSystem() | { sys =>
      val hello = sys.create(HelloActor(_))
      hello.ask("world").whenComplete { (m, e) =>
        println(m.value)
        cd.countDown()
      }

      cd.await()
    }
  }

}
