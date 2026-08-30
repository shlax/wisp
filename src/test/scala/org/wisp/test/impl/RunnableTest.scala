package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.Sink
import org.wisp.stream.extensions.*
import org.wisp.stream.graph.StreamGraph
import org.wisp.utils.extensions.*

import scala.concurrent.duration.*
import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.*

class RunnableTest {

  @Test
  def runnable(): Unit = {
    val thread = Thread.currentThread()

    val data = Seq(0, 1, 2, 3, 4, 5).asSource.map{ i =>
      Assertions.assertTrue(Thread.currentThread() == thread)
      i
    }

    val l = Collections.synchronizedList(new util.ArrayList[Int]())

    val cnt = new AtomicInteger()
    val sink = new Sink[Int] {
      override def apply(o: Option[Int]): Unit = o match {
        case Some(t) =>
          Assertions.assertTrue(Thread.currentThread() == thread)
          l.add(t)
        case None =>
          Assertions.assertTrue(Thread.currentThread() == thread)
          cnt.incrementAndGet()
      }
    }

    ActorSystem() || { sys =>
      val r = StreamGraph().runnable(data, sink)(identity)
      r.run()
    }

    Assertions.assertEquals(0 to 5, l.asScala)
    Assertions.assertEquals(1, cnt.get())

  }

  @Test
  def runnableTransformerFilter(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[Int]())
    val acc = new AtomicInteger()

    ActorSystem() || { sys =>

      val tId = Thread.currentThread()

      val data = Seq(0, 1, 2, 3, 4, 5)

      val sink = new Sink[Int] {
        override def apply(o: Option[Int]): Unit = o match {
          case Some(t) =>
            l.add(t)
          case None =>
            acc.incrementAndGet()
        }
      }

      val g = StreamGraph()
      var futures: List[Future[Unit]] = Nil

      val src = g.fromRunnable(data.asSource){ s =>
        val w1 = s.filterTo( (q: Int) => q % 2 == 0 )
        val w2 = s.filterTo( (q: Int) => q % 2 == 0 )

        futures = w1.start :: futures
        futures = w2.start :: futures

        futures = g.zip(w1, w2).toRunnable(sink).start :: futures

      }

      src.run()

      for(f <- futures){
        Await.ready(f, 1.second)
      }

    }

    Assertions.assertEquals(List(0, 2, 4).toSet, l.asScala.toSet)
    Assertions.assertEquals(1, acc.get())
  }

}
