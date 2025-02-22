package org.wisp.test

import org.junit.jupiter.api.Test
import org.wisp.stream.iterator.{ActorFlow, ActorSink, ActorSource}
import org.wisp.ActorSystem
import org.wisp.using.*
import org.wisp.stream.Source.*

import scala.util.Random

class HelloActorFlow {

  @Test
  def test(): Unit = {
    new ActorSystem(3) | { sys =>
      val data = Seq(0,1,2,3,4,5)
      val src = ActorSource(data.asSource, sys)

      val w1 = sys.create( i => ActorFlow(src, i, { q =>
        Thread.sleep(Random.nextInt(50))
        "w1:" + Thread.currentThread().threadId + ":" + q
      }))

      val w2 = sys.create(i => ActorFlow(src, i, { q =>
        Thread.sleep(Random.nextInt(25))
        "w2:" + Thread.currentThread().threadId + ":" + q
      }))

      ActorSink(Seq(w1, w2), println(_)).start().get()

    }
  }

}
