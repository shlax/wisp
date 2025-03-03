package org.wisp.test.stream.iterator

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.stream.Sink
import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.iterator.{ForEachSink, StreamWorker}
import org.wisp.using.*
import org.wisp.stream.Source.*

class HelloForEachSink {

  @Test
  def test():Unit = {
    ActorSystem() | ( _.as { sys =>

      val tId = Thread.currentThread().threadId

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread().threadId == tId)
        "s[" + Thread.currentThread().threadId + "]:" + i
      }

      val sink = new Sink[String]{
        override def accept(t: String): Unit = {
          Assertions.assertTrue(Thread.currentThread().threadId == tId)
          println("d[" + Thread.currentThread().threadId + "]:" + t)
        }
      }

      val src = ForEachSink(data, sink){ (ref:ActorLink) =>
        sys.create(i => StreamWorker.map(ref, i, (q :String) =>
          "w:" + Thread.currentThread().threadId + ":" + q
        ))
      }

      src.run()

    })
  }

}
