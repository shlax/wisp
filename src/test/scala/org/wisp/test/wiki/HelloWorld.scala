package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.utils.closeable.*
import org.wisp.{AbstractActor, Link, ActorSystem, ActorScheduler}

class HelloWorld {

  @Test
  def helloWorld():Unit = {

    class HelloActor(inbox:ActorScheduler[String, Nothing]) extends AbstractActor(inbox){
      override def apply(from: Link[Nothing, String]): String => Unit = println
    }

    // create ActorSystem and close it
    ActorSystem() | { system =>
      //  create hello actor
      val link : Link[String, Nothing] = system.create(HelloActor(_))
      //  send message
      link << "Hello world"
    }

  }

}
