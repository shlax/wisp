package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.{AbstractActor, Link, ActorSystem, ActorScheduler}
import org.wisp.utils.closeable.*

import scala.concurrent.ExecutionContext

class AskHelloWorld {

  @Test
  def askHelloWorld(): Unit = {

    class HelloActor(inbox: ActorScheduler[Any, Any]) extends AbstractActor(inbox) {
      override def apply(from: Link[Any, Any]): PartialFunction[Any, Unit] = {
        case a =>
          // send message back
          from << "Hello "+a
      }
    }

    // create ActorSystem and close it
    ActorSystem() | { system =>
      given ExecutionContext = system

      //  create hello actor
      val link = system.create(HelloActor(_))

      //  send message
      link.ask("world").onComplete{ v =>
        // precess response
        println(v.get)
      }

    }

  }

}
