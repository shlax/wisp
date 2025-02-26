package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.iterator.{StreamWorker, StreamSink, StreamSource, StreamBuffer, ZipStream}
import org.wisp.using.*
import org.wisp.stream.Source.*

import scala.util.Random

class HelloZipStream {

  @Test
  def test():Unit = {
    ActorSystem() | { sys =>
      val data = Seq(0, 1, 2, 3, 4).asSource
      val src = StreamSource(data)

      val w1 = sys.create(i => StreamWorker.map(src, i, { q =>
        println("w1:start")
        Thread.sleep(Random.nextInt(100))
        "w1:" + Thread.currentThread().threadId + ":" + q
      }))

      val w2 = sys.create(i => StreamWorker.map(src, i, { q =>
        println("w2:start")
        Thread.sleep(Random.nextInt(50))
        "w2:" + Thread.currentThread().threadId + ":" + q
      }))

      val r = ZipStream(w1, w2)

      StreamSink(r, println(_)).start().get()

    }
  }

}
