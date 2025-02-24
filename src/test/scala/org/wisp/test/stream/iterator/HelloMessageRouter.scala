package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.iterator.{ActorFlow, ActorSink, ActorSource, MessageRouter}
import org.wisp.using.*
import org.wisp.stream.Source.*

import scala.util.Random

class HelloMessageRouter {

  @Test
  def test():Unit = {
    ActorSystem() | { sys =>
      val data = Seq(0).asSource
      val src = ActorSource(data)

      val w1 = sys.create(i => ActorFlow(src, i, { q =>
        Thread.sleep(Random.nextInt(50))
        "w1:" + Thread.currentThread().threadId + ":" + q
      }))

      val w2 = sys.create(i => ActorFlow(src, i, { q =>
        Thread.sleep(Random.nextInt(50))
        "w2:" + Thread.currentThread().threadId + ":" + q
      }))

      val r = MessageRouter(w1, w2)

      ActorSink(r, println(_)).start().get()

    }
  }

}
