package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.Sink
import org.wisp.stream.extensions.*
import org.wisp.stream.iterator.{RunnableSourceSink, RunnableSource, RunnableSink, SplitStream, StreamBuffer, StreamSink, StreamSource, StreamWorker, ZipStream}
import org.wisp.stream.typed.StreamGraph
import tests.*

import java.util.Collections
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future, Promise}
import scala.util.Success
import java.util
import scala.concurrent.duration.*

class EmptyTests {

  @Test
  def sinkFlatMap():Unit = {
    val data = Seq(List(),List()).asSource
    val l = ArrayBuffer[Int]()

    val x = Sink[Int](i => l += i)
    val y = x.flatMap[List[Int]]{ (t, self) =>
      for (i <- t) self.accept(i)
    }

    data.forEach(y)
    Assertions.assertTrue(l.isEmpty)
  }

  @Test
  def sinkFold(): Unit = {
    val data = Seq[Int]().asSource
    
    val (s, f) = Promise[Int]().asSink[Int](0){ (a, b) => a + b }
    Assertions.assertFalse(f.isCompleted)
    data.forEach(s)

    Assertions.assertEquals(Some(Success(0)), f.value)
  }

  @Test
  def sourceFlatMap(): Unit = {
    val data = Seq[List[Int]](List(), List()).asSource
    val l = ArrayBuffer[Int]()

    data.flatMap(i => i.asSource).each(i => l += i)

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

    ActorSystem() || { sys =>
      val p = StreamGraph(sys).from(data).map(i => i + 1).to(l.add).start()
      Await.result(p, 1.second)
    }

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def typedSinkTree(): Unit = {
    val data = Seq[Int]().asSource

    val l1 = Collections.synchronizedList(new util.ArrayList[String]())
    val l2 = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val s1 = Sink[String](l1.add).map[Int]("a:" + _).map[Int](i => i * 2 + 0)
      val s2 = Sink[String](l2.add).map[Int]("b:" + _).map[Int](i => i * 2 + 1)
      val t = s1.thenTo(s2)

      val p = StreamGraph(sys).from(data).map(i => i + 1).to(t).start()
      Await.result(p, 1.second)
    }

    Assertions.assertTrue(l1.isEmpty)
    Assertions.assertTrue(l2.isEmpty)

  }

  @Test
  def forEachSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val thread = Thread.currentThread()

      val data = Seq[Int]().asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread() == thread)
        "s:" + i
      }

      val sink = new Sink[String] {
        override def accept(t: String): Unit = {
          Assertions.assertTrue(Thread.currentThread() == thread)
          l.add("d:" + t)
        }
      }

      val src = RunnableSourceSink(data, sink) { (ref: ActorLink) =>
        sys.create(i => StreamWorker.map(ref, i, (q: String) =>
          "w:" + q
        ))
      }

      src.run()

    }

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def forEachSource(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val thread = Thread.currentThread()

      val data = Seq[Int]().asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread() == thread)
        "s:" + i
      }
      val src = RunnableSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      src.failOn(p).run()
      Await.ready(p, 1.second)

    }

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def runnableSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq[Int]().asSource

      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      RunnableSink(w, l.add).run()

    }

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def streamBuffer(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq[Int]().asSource

      val src = StreamSource(data.map { i => i })

      val b = StreamBuffer(src, 3)

      val w = sys.create(i => StreamWorker.map(b, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      Await.ready(p, 1.second)
    }

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def streamWorker(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq[Int]().asSource
      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      Await.ready(p, 1.second)

    }

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def zipStream(): Unit = {
    val l = Collections.synchronizedSet(new util.HashSet[String]())

    ActorSystem() || { sys =>

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
      Await.ready(p, 1.second)

    }

    Assertions.assertTrue(l.isEmpty)

  }

  @Test
  def splitStream(): Unit = {
    val l1 = Collections.synchronizedSet(new util.HashSet[Integer]())
    val l2 = Collections.synchronizedSet(new util.HashSet[Integer]())

    ActorSystem() || { sys =>

      val data = Seq[Int]().asSource
      val src = StreamSource(data)

      var sl: List[StreamSink[?]] = Nil

      val r = SplitStream(src) { b =>
        sl = StreamSink(b.next(), l1.add) :: sl
        sl = StreamSink(b.next(), l2.add) :: sl
      }

      val p = Future.sequence(sl.map(_.start()))
      Await.ready(p, 1.second)

    }

    Assertions.assertTrue(l1.isEmpty)
    Assertions.assertTrue(l2.isEmpty)

  }

}
