package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.extensions.*
import org.wisp.stream.typed.StreamGraph
import org.wisp.test.impl.testSystem.*

import java.util
import java.util.Collections
import scala.jdk.CollectionConverters.*

class RunnableTest {

  @Test
  def runnable(): Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource
    val l = Collections.synchronizedList(new util.ArrayList[Int]())

    ActorSystem() || { sys =>
      val r = StreamGraph(sys).runnable(data, l.add)(identity)
      r.run()
    }

    Assertions.assertEquals(0 to 5, l.asScala)

  }

}
