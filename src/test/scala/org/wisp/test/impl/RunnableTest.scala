package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.Sink
import org.wisp.stream.extensions.*
import org.wisp.stream.typed.StreamGraph
import org.wisp.test.impl.tests.*

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.*

class RunnableTest {

  @Test
  def runnable(): Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource
    val l = Collections.synchronizedList(new util.ArrayList[Int]())

    val thread = Thread.currentThread()

    val cnt = new AtomicInteger()
    val sink = new Sink[Int] {
      override def apply(t: Int): Unit = {
        Assertions.assertTrue(Thread.currentThread() == thread)
        l.add(t)
      }
      override def complete(): Unit = {
        Assertions.assertTrue(Thread.currentThread() == thread)
        cnt.incrementAndGet()
      }
    }

    ActorSystem() || { sys =>
      val r = StreamGraph(sys).runnable(data, sink)(identity)
      r.run()
    }

    Assertions.assertEquals(0 to 5, l.asScala)
    Assertions.assertEquals(1, cnt.get())

  }

}
