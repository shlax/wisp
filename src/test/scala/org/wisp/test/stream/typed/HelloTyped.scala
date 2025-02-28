package org.wisp.test.stream.typed

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.typed.StreamGraph
import org.wisp.using.*
import org.wisp.stream.Source.*

import scala.concurrent.Await
import scala.concurrent.duration.*

class HelloTyped {

  @Test
  def test():Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource

    ActorSystem() | ( _.as { sys =>
      val p = StreamGraph(sys).from(data).map( i => i + 1).to(println).start()
      Await.result(p.future, 1.second)
    })

  }

}
