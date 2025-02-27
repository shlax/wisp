package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{StreamSink, StreamSource, StreamWorker}
import org.wisp.using.*

import scala.concurrent.{Await, ExecutionContextExecutorService}
import scala.concurrent.duration.*

class HelloStreamWorker {

  @Test
  def test(): Unit = {
    ActorSystem() | { implicit sys =>

      val data = Seq(0,1,2,3,4,5).asSource
      val src = StreamSource(data)

      val w = sys.create( i => StreamWorker.map(src, i, q =>
        "w:" + Thread.currentThread().threadId + ":" + q
      ))

      val p = StreamSink(w, println(_)).start()
      Await.ready(p.future, 1.second)
    }

  }

}
