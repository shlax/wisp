package org.wisp.test.stream.iterator

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{StreamWorker, StreamSink, ForEachSource}
import org.wisp.using.*

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
      val src = ForEachSource(data)

      val w = sys.create(i => StreamWorker.map(src, i){ q =>
        Thread.sleep(Random.nextInt(50))
        "w:" + Thread.currentThread().threadId + ":" + q
      })

      val cf = StreamSink(w, println(_)).start()
      Thread.sleep(50)
      println("start")

      src.run()
      cf.get()

    }
  }

}
