package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.using.*
import org.wisp.{AbstractActor, ActorLink, ActorSystem, Inbox}

class HelloWorld {

  @Test
  def helloWorld():Unit = {

    class HelloActor(inbox:Inbox) extends AbstractActor(inbox){
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a => println(a)
      }
    }

    // create ActorSystem and close it
    ActorSystem() | { system =>
      //  create hello actor
      val link = system.create(HelloActor(_))
      //  send message
      link << "Hello world"
    }

  }

}
