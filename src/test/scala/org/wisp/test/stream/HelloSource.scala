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
  def fold():Unit = {
    val data = Seq(1, 2, 3).asSource

    val r = data.fold(0)( (a, b) => a + b)
    println(r)
  }

}
