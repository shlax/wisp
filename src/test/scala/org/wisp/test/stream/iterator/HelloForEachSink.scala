package org.wisp.test.stream.iterator

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.iterator.{StreamWorker, ForEachSink}
import org.wisp.using.*
import org.wisp.stream.Source.*

import java.util.function.Consumer

class HelloForEachSink {

  @Test
  def test():Unit = {
    ActorSystem() | { sys =>
      val tId = Thread.currentThread().threadId

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread().threadId == tId)
        "s[" + Thread.currentThread().threadId + "]:" + i
      }

      val sink = new Consumer[Any]{
        override def accept(t: Any): Unit = {
          Assertions.assertTrue(Thread.currentThread().threadId == tId)
          println("d[" + Thread.currentThread().threadId + "]:" + t)
        }
      }

      val src:ForEachSink = ForEachSink(sys, data, sink){ (ref:ActorLink) =>
        sys.create(i => StreamWorker(ref, i, { q =>
          "w:" + Thread.currentThread().threadId + ":" + q
        }))
      }

      src.run()

    }
  }

}
