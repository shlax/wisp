package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.{Sink, SinkTree}
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{ForEachSink, ForEachSource, RunnableSink, StreamBuffer, StreamSink, StreamSource, StreamWorker, ZipStream}
import org.wisp.stream.typed.StreamGraph
import org.wisp.using.*

import java.util.Collections
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Promise}
import scala.util.Success
import java.util
import scala.concurrent.duration.*

class EmptyTests {

  @Test
  def sinkFlatMap():Unit = {
    val data = Seq(List(),List()).asSource
    val l = ArrayBuffer[Int]()

    SinkTree(data){ f =>
      f.flatMap{ (x:Sink[Int]) =>
        (t: List[Int]) => for (i <- t) x.accept(i)
      }.map(i => l += i)
    }

    Assertions.assertTrue(l.isEmpty)
  }

  @Test
  def sinkFold(): Unit = {
    val data = Seq[Int]().asSource

    val p: Promise[Int] = SinkTree(data) { f =>
      val tmp = f.fold(0)((a, b) => a + b)
      Assertions.assertFalse(tmp.isCompleted)
      tmp
    }

    Assertions.assertEquals(Some(Success(0)), p.future.value)
  }

  @Test
  def sourceFlatMap(): Unit = {
    val data = Seq[List[Int]](List(), List()).asSource
    val l = ArrayBuffer[Int]()

    data.flatMap(i => i.asSource).forEach(i => l += i)

    Assertions.assertTrue(l.isEmpty)
  }

  @Test
  def sourceFold(): Unit = {
    val data = Seq[Int]().asSource

    val r = data.fold(0)((a, b) => a + b)

    Assertions.assertEquals(0, r)
  }

  @Test
  def typedStreamGraph(): Unit = {
    val data = Seq[Int]().asSource
    val l = Collections.synchronizedList(new util.ArrayList[Int]())

    ActorSystem() | (_.as { sys =>
      val p = StreamGraph(sys).from(data).map(i => i + 1).to(l.add).start()
      Await.result(p.future, 1.second)
    })

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def typedSinkTree(): Unit = {
    val data = Seq[Int]().asSource

    val l1 = Collections.synchronizedList(new util.ArrayList[String]())
    val l2 = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val t = SinkTree[Int] { x =>
        x.as { y =>
          y.map(i => i * 2 + 0).map("a:" + _).to(l1.add)
          y.map(i => i * 2 + 1).map("b:" + _).to(l2.add)
        }
      }

      val p = StreamGraph(sys).from(data).map(i => i + 1).to(t).start()
      Await.result(p.future, 1.second)
    })

    Assertions.assertTrue(l1.isEmpty)
    Assertions.assertTrue(l2.isEmpty)

  }

  @Test
  def forEachSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val tId = Thread.currentThread().threadId

      val data = Seq[Int]().asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread().threadId == tId)
        "s:" + i
      }

      val sink = new Sink[String] {
        override def accept(t: String): Unit = {
          Assertions.assertTrue(Thread.currentThread().threadId == tId)
          l.add("d:" + t)
        }
      }

      val src = ForEachSink(data, sink) { (ref: ActorLink) =>
        sys.create(i => StreamWorker.map(ref, i, (q: String) =>
          "w:" + q
        ))
      }

      src.run()

    })

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def forEachSource(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val tId = Thread.currentThread().threadId

      val data = Seq[Int]().asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread().threadId == tId)
        "s:" + i
      }
      val src = ForEachSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start().future
      src.failOn(p).run()
      Await.ready(p, 1.second)

    })

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def runnableSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val data = Seq[Int]().asSource

      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      RunnableSink(w, l.add).run()

    })

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def streamBuffer(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val data = Seq[Int]().asSource

      val src = StreamSource(data.map { i => i })

      val b = StreamBuffer(src, 3)

      val w = sys.create(i => StreamWorker.map(b, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      Await.ready(p.future, 1.second)
    })

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def streamWorker(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val data = Seq[Int]().asSource
      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      Await.ready(p.future, 1.second)

    })

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def zipStream(): Unit = {
    val l = Collections.synchronizedSet(new util.HashSet[String]())

    ActorSystem() | (_.as { sys =>

      val data = Seq[Int]().asSource
      val src = StreamSource(data)

      val w1 = sys.create(i => StreamWorker.map(src, i, q => {
        "w:" + q
      }))

      val w2 = sys.create(i => StreamWorker.map(src, i, q => {
        "w:" + q
      }))

      val r = ZipStream(w1, w2)

      val p = StreamSink(r, l.add).start()
      Await.ready(p.future, 1.second)

    })

    Assertions.assertTrue(l.isEmpty)

  }


}
