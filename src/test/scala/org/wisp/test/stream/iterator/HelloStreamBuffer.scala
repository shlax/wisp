package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.using.*
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{StreamSink, StreamSource, StreamBuffer, StreamWorker}

import scala.util.Random

class HelloStreamBuffer {

  @Test
  def test():Unit = {
    ActorSystem() | { sys =>
      val data = Seq(0, 1, 2, 3, 4, 5).asSource

      val src = StreamSource(data.map { i =>
        println("send:"+i)
        i
      })

      val b = StreamBuffer(src, 3)

      val w = sys.create(i => StreamWorker.map(b, i){ q =>
        Thread.sleep(Random.nextInt(50))
        "w:" + Thread.currentThread().threadId + ":" + q
      })

      StreamSink(w, println(_)).start().get()

    }
  }

}
