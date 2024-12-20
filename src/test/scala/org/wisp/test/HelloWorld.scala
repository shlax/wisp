package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorRef, ActorSystem, Inbox}
import org.wisp.using.*

import java.util.concurrent.CountDownLatch

class HelloWorld {

  val cd = new CountDownLatch(1)

  class HelloActor(in:Inbox) extends Actor(in){
    override def accept(from: ActorRef): PartialFunction[Any, Unit] = {
      case a =>
        println(a)
        cd.countDown()
    }
  }

  @Test
  def test(): Unit = {
    new ActorSystem(3)|{ sys =>
      val hello = sys.create(HelloActor(_))
      hello << "Hello world"

      cd.await()
    }
  }

}
