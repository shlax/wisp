package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.using.*
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{StreamBuffer, StreamSink, StreamSource, StreamWorker}

import scala.concurrent.Await
import scala.concurrent.duration.*

class HelloStreamBuffer {

  @Test
  def test():Unit = {
    ActorSystem() | ( _.as { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource

      val src = StreamSource(data.map { i =>
        println("send:"+i)
        i
      })

      val b = StreamBuffer(src, 3)

      val w = sys.create(i => StreamWorker.map(b, i, q =>
        "w:" + Thread.currentThread().threadId + ":" + q
      ))

      val p = StreamSink(w, println(_)).start()
      Await.ready(p.future, 1.second)

    })
  }

}
