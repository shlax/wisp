package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.{Actor, ActorRef, ActorSystem, Inbox}
import org.wisp.using.*

import java.time.Duration

class HelloAsk {

  class HelloActor(in:Inbox) extends Actor(in){
    override def accept(from: ActorRef): PartialFunction[Any, Unit] = {
      case a => from ! "Hello "+a
    }
  }

  @Test
  def test():Unit = {
    new ActorSystem(3) | { sys =>
      val hello = sys.create(HelloActor(_))
      (hello ? "world").whenComplete( (m, e) => println(m.message) )

      Thread.sleep(Duration.ofSeconds(5))
    }
  }

}
