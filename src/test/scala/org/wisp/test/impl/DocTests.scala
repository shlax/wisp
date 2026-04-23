package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.extensions.*
import org.wisp.stream.graph.StreamGraph
import org.wisp.utils.closeable.*
import org.wisp.utils.extensions.*

import java.util
import java.util.Collections
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class DocTests {

  @Test
  def zipTest():Unit = {
    val res = Collections.synchronizedSet(util.HashSet[Int]())
    new ActorSystem() || { as =>
      val graph = new StreamGraph()
      val source1 = graph.from( (0 until 5).asSource.map(i => i * 2) )
      val source2 = graph.from( (0 until 5).asSource.map(i => i * 2 + 1) )
      val future:Future[Unit] = graph.zip(source1, source2).to(i => res.add(i)).start
      Await.ready(future, 1.second)
    }
    Assertions.assertEquals((0 until 10).toSet, res.asScala.toSet)
  }

  @Test
  def splitTest(): Unit = {
    val res1 = Collections.synchronizedSet(util.HashSet[Int]())
    val res2 = Collections.synchronizedSet(util.HashSet[Int]())
    new ActorSystem() || { as =>
      val source = new StreamGraph().from((0 until 5).asSource)
      val future = source.split{ s =>
        val f1 = s.copy.map(i => i * 2).to(i => res1.add(i)).start
        val f2 = s.copy.map(i => i * 2 + 1).to(i => res2.add(i)).start
        Future.sequence(Seq(f1, f2))
      }
      Await.ready(future, 1.second)
    }
    Assertions.assertEquals(Set(0, 2, 4, 6, 8), res1.asScala.toSet)
    Assertions.assertEquals(Set(1, 3, 5, 7, 9), res2.asScala.toSet)
  }

}
