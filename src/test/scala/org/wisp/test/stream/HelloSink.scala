package org.wisp.test.stream

import org.junit.jupiter.api.Test
import org.wisp.stream.{Sink, SinkTree}
import org.wisp.stream.Source.*

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.*

class HelloSink {

  @Test
  def flatMap():Unit = {
    val data = Seq(List(0,1,2),List(3,4,5)).asSource

    SinkTree(data){ f =>
      f.flatMap{ (x:Sink[Int]) =>
        (t: List[Int]) => for (i <- t) x.accept(i)
      }.map(println)
    }
  }

  @Test
  def fold():Unit = {
    val data = Seq(1, 2, 3).asSource

    val p:Promise[Int] = SinkTree(data){ f =>
      val tmp = f.fold(0)( (a, b) => a + b)
      println(tmp.isCompleted)
      tmp
    }

    println(p.future.value)

  }

}
