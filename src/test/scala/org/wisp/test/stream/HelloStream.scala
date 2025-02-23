package org.wisp.test.stream

import org.junit.jupiter.api.Test
import org.wisp.stream.Stream
import org.wisp.stream.Source.*

import java.util.function.Consumer

class HelloStream {

  @Test
  def flatMap():Unit = {
    val data = Seq(List(0,1,2),List(3,4,5)).asSource

    Stream(data){ f =>
      f.flatMap{ (x:Consumer[Int]) =>
        (t: List[Int]) => for (i <- t) x.accept(i)
      }.map(println)
    }
  }

  @Test
  def groupBy():Unit = {
    val data = Seq(("a", 1), ("a", 2), ("b", 3), ("b", 4)).asSource

    Stream(data){ f =>
      f.groupBy(_._1, (c:Option[List[Int]], i:(String,Int)) => {
        i._2 :: c.getOrElse(Nil)
      } ).map(println)
    }
  }

}
