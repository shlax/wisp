package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorRef, ActorSystem, Inbox, Message}
import org.wisp.using.*

import java.util.concurrent.CountDownLatch

class HelloAsk {

  val cd = new CountDownLatch(1)

  class HelloActor(in:Inbox) extends Actor(in){
    override def accept(from: ActorRef): PartialFunction[Any, Unit] = {
      case a => from ! "Hello "+a
    }
  }

  @Test
  def test():Unit = {
    ActorSystem(3) | { sys =>
      val hello = sys.create(HelloActor(_))
      (hello ? "world").whenComplete { (m, e) =>
        println(m.message)
        cd.countDown()
      }

      cd.await()
    }
  }

}
