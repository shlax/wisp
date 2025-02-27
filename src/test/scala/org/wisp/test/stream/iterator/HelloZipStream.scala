package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.iterator.{StreamSink, StreamSource, StreamWorker, ZipStream}
import org.wisp.using.*
import org.wisp.stream.Source.*

import scala.concurrent.{Await, ExecutionContextExecutorService}
import scala.concurrent.duration.*

class HelloZipStream {

  @Test
  def test():Unit = {
    ActorSystem() | { implicit sys =>

      val data = Seq(0, 1, 2, 3, 4).asSource
      val src = StreamSource(data)

      val w1 = sys.create(i => StreamWorker.map(src, i, q => {
        println("w1:start")
        "w1:" + Thread.currentThread().threadId + ":" + q
      }))

      val w2 = sys.create(i => StreamWorker.map(src, i, q => {
        println("w2:start")
        "w2:" + Thread.currentThread().threadId + ":" + q
      }))

      val r = ZipStream(w1, w2)

      val p = StreamSink(r, println(_)).start()
      Await.ready(p.future, 1.second)

    }
  }

}
