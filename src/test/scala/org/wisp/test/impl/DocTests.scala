package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.extensions.*
import org.wisp.stream.typed.StreamGraph
import org.wisp.utils.closeable.*

import java.util
import java.util.Collections
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class DocTests {

  @Test
  def zipTest():Unit = {
    val res = Collections.synchronizedSet(util.HashSet[Int]())
    new ActorSystem()|{ as =>
      val graph = new StreamGraph(as)
      val source1 = graph.from( (0 until 5).asSource.map(i => i * 2) )
      val source2 = graph.from( (0 until 5).asSource.map(i => i * 2 + 1) )
      val future:Future[Unit] = graph.zip(source1, source2).to(i => res.add(i)).start
      Await.ready(future, 1.minute)
    }
    Assertions.assertEquals((0 until 10).toSet, res.asScala.toSet)
  }

}
