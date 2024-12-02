package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorRef, ActorSystem, Inbox}
import org.wisp.using.*

import java.time.Duration

class HelloWorld {

  class HelloActor(in:Inbox) extends Actor(in){
    override def accept(from: ActorRef): PartialFunction[Any, Unit] = {
      case a => println(a)
    }
  }

  @Test
  def test(): Unit = {

    new ActorSystem(3)|{ sys =>
      val hello = sys.create(HelloActor(_))
      hello ! "Hello world"

      Thread.sleep(Duration.ofSeconds(5))
    }

  }

}
