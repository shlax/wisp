package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.using.*
import org.wisp.{Actor, ActorLink, ActorSystem, Inbox}

class HelloWorld {

  @Test
  def helloWorld():Unit = {

    class HelloActor(inbox:Inbox) extends Actor(inbox){
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a => println(a)
      }
    }

    // create ActorSystem and close it
    ActorSystem() | { system =>
      // send message
      system.create(HelloActor(_)) << "Hello world"
    }

  }

}
