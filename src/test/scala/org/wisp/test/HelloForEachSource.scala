package org.wisp.test

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.iterator.{ActorFlow, ActorSink, ForEachSource}
import org.wisp.using.*
import org.wisp.stream.Source.*

import scala.util.Random

class HelloForEachSource {

  @Test
  def test(): Unit = {
    ActorSystem() | { sys =>
      val tId = Thread.currentThread().threadId
      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map{ i =>
        Assertions.assertTrue(Thread.currentThread().threadId == tId)
        "s["+Thread.currentThread().threadId+"]:"+i
      }

      val src = ForEachSource(data, sys)

      val w1 = sys.create(i => ActorFlow(src, i, { q =>
        Thread.sleep(Random.nextInt(50))
        "w1:" + Thread.currentThread().threadId + ":" + q
      }))

      val w2 = sys.create(i => ActorFlow(src, i, { q =>
        Thread.sleep(Random.nextInt(25))
        "w2:" + Thread.currentThread().threadId + ":" + q
      }))

      val cf = ActorSink(Seq(w1, w2), println(_)).start()
      Thread.sleep(50)
      println("start")

      src.run()
      cf.get()

    }
  }

}
