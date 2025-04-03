package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.closeable.*
import org.wisp.{AbstractActor, ActorLink, ActorSystem, ActorScheduler}

class HelloWorld {

  @Test
  def helloWorld():Unit = {

    class HelloActor(inbox:ActorScheduler) extends AbstractActor(inbox){
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
