package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.using.*
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{ActorSink, ActorSource, StreamBuffer, StreamWorker}

import scala.util.Random

class HelloStreamBuffer {

  @Test
  def test():Unit = {
    ActorSystem() | { sys =>
      val data = Seq(0, 1, 2, 3, 4, 5).asSource

      val src = ActorSource(data.map { i =>
        println("send:"+i)
        i
      })

      val b = StreamBuffer(sys, src, 3)

      val w = sys.create(i => StreamWorker(b, i, { q =>
        Thread.sleep(Random.nextInt(50))
        "w:" + Thread.currentThread().threadId + ":" + q
      }))

      ActorSink(sys, w, println(_)).start().get()

    }
  }

}
