package org.wisp.test.stream

import org.junit.jupiter.api.Test
import org.wisp.stream.Flow
import org.wisp.stream.Source.*

import java.util.function.Consumer

class HelloFlow {

  @Test
  def flatMap():Unit = {
    val data = Seq(List(0,1,2),List(3,4,5)).asSource

    Flow(data){ f =>
      f.flatMap{ (x:Consumer[Int]) =>
        (t: List[Int]) => for (i <- t) x.accept(i)
      }.map(println)
    }
  }

}
