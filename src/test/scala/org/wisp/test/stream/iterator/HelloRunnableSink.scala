package org.wisp.test.stream.iterator

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.using.*
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{RunnableSink, StreamSource, StreamWorker}

class HelloRunnableSink {

  @Test
  def test(): Unit = {
    ActorSystem() | ( _.as { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource

      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + Thread.currentThread().threadId + ":" + q
      ))

      RunnableSink(w, println(_)).run()

    })
  }

}
