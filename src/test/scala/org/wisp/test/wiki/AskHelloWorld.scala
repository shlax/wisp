package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.{AbstractActor, ActorScheduler, ActorSystem, Link}
import org.wisp.utils.extensions.||
import scala.util.Try

class AskHelloWorld {

  @Test
  def askHelloWorld(): Unit = {

    class SquaredActor(inbox: ActorScheduler[Int, String]) extends AbstractActor(inbox) {
      override def apply(from: Link[String, Int]): Int => Unit = { i =>
          // send message back
          from << s"$i squared is ${i * i}"
      }
    }

    // create ActorSystem and close it
    ActorSystem() || { system =>
      // given ExecutionContext = system

      //  create hello actor
      val link : Link[Int, String] = system.create(SquaredActor(_))

      //  send message
      link.ask(2).onComplete{ (v : Try[String]) =>
        // precess response
        println(v.get)
      }

    }

  }

}
