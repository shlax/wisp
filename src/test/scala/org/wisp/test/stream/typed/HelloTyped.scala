package org.wisp.test.stream.typed

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.SinkTree
import org.wisp.stream.typed.StreamGraph
import org.wisp.using.*
import org.wisp.stream.Source.*

import scala.concurrent.Await
import scala.concurrent.duration.*

class HelloTyped {

  @Test
  def streamGraph():Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource

    ActorSystem() | ( _.as { sys =>
      val p = StreamGraph(sys).from(data).map(i => i + 1).to(println).start()
      Await.result(p.future, 1.second)
    })

  }

  @Test
  def sinkTree():Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource

    ActorSystem() | ( _.as { sys =>

      val t = SinkTree[Int] { x =>
        x.as { y =>
          y.map(i => i * 2 + 0).map("a:" + _).to(println)
          y.map(i => i * 2 + 1).map("b:" + _).to(println)
        }
      }

      val p = StreamGraph(sys).from(data).map(i => i + 1).to(t).start()
      Await.result(p.future, 1.second)
    })

  }

}
