package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{StreamWorker, StreamSink, StreamSource}
import org.wisp.using.*

import scala.util.Random

class HelloStreamWorker {

  @Test
  def test(): Unit = {
    ActorSystem() | { sys =>
      val data = Seq(0,1,2,3,4,5).asSource
      val src = StreamSource(data)

      val w = sys.create( i => StreamWorker.map(src, i){ q =>
        Thread.sleep(Random.nextInt(50))
        "w:" + Thread.currentThread().threadId + ":" + q
      })

      StreamSink(w, println(_)).start().get()

    }
  }

}
