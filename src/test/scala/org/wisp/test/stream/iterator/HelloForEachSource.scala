package org.wisp.test.stream.iterator

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{ForEachSource, StreamSink, StreamWorker}
import org.wisp.using.*

import scala.concurrent.Await
import scala.concurrent.duration.*

class HelloForEachSource {

  @Test
  def test(): Unit = {
    ActorSystem() | ( _.as { sys =>

      val tId = Thread.currentThread().threadId

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map{ i =>
        Assertions.assertTrue(Thread.currentThread().threadId == tId)
        "s["+Thread.currentThread().threadId+"]:"+i
      }
      val src = ForEachSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + Thread.currentThread().threadId + ":" + q
      ))

      val p = StreamSink(w, println(_)).start()
      println("start")

      src.run()
      Await.ready(p.future, 1.second)

    })
  }

}
