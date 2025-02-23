package org.wisp.test.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{ActorFlow, ActorSink, ActorSource}
import org.wisp.using.*

import scala.util.Random

class HelloActorFlow {

  @Test
  def test(): Unit = {
    ActorSystem() | { sys =>
      val data = Seq(0,1,2,3,4,5).asSource
      val src = ActorSource(data)

      val w = sys.create( i => ActorFlow(src, i, { q =>
        Thread.sleep(Random.nextInt(50))
        "w:" + Thread.currentThread().threadId + ":" + q
      }))

      ActorSink(w, println(_)).start().get()

    }
  }

}
