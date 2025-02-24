package org.wisp.test.stream

import org.junit.jupiter.api.Test
import org.wisp.stream.Source.*

class HelloSource {

  @Test
  def flatMap(): Unit = {
    val data = Seq(List(0, 1, 2), List(3, 4, 5)).asSource

    data.flatMap( i => i.asSource ).forEach(println)
  }

  @Test
  def groupBy():Unit = {
    val data = Seq(("a", 1), ("a", 2), ("b", 3), ("b", 4)).asSource

    data.groupBy(_._1, (o:Option[List[Int]], i:(String, Int)) => {
        i._2 :: o.getOrElse(Nil)
      } ).forEach(println)
  }

}
