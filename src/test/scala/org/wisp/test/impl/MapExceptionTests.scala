package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{Link, ActorSystem}
import org.wisp.stream.Sink
import org.wisp.utils.extensions.*
import org.wisp.stream.extensions.*
import org.wisp.stream.iterator.{RunnableSourceSink, RunnableSource, RunnableSink, StreamBuffer, StreamSink, StreamSource, StreamTransformer, ZipStream}

import java.util
import java.util.Collections
import scala.concurrent.Await
import scala.jdk.CollectionConverters.*
import scala.concurrent.duration.*

class MapExceptionTests {
  class MyException(msg: String) extends RuntimeException(msg)

  @Test
  def forEachSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val thread = Thread.currentThread()

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread() == thread)
        "s:" + i
      }

      val sink = new Sink[String] {
        override def apply(t: String): Unit = {
          Assertions.assertTrue(Thread.currentThread() == thread)
          l.add(t)
        }
      }

      val src = RunnableSourceSink(data, sink) { ref =>
        StreamTransformer.map(ref, (q: String) =>
          if (q == "s:4") throw new MyException("is 4")
          "w:" + q
        )
      }

      src.run()

    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3", "w:s:5"), l.asScala)

  }

  @Test
  def forEachSource(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = RunnableSource(data)

      val w = StreamTransformer.map(src, q =>
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      )

      val f = StreamSink(w, l.add).start
      
      src.failOn(f).run()
     
      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3", "w:s:5"), l.asScala)

  }

  @Test
  def runnableSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = StreamSource(data)

      val w = StreamTransformer.map(src, q =>
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      )

      RunnableSink(w, l.add).run()

    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3", "w:s:5"), l.asScala)
  }

  @Test
  def streamWorker(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = StreamSource(data)

      val w = StreamTransformer.map(src, q =>
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      )

      val f = StreamSink(w, l.add).start

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3", "w:s:5"), l.asScala)

  }

  @Test
  def streamBuffer(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = StreamSource(data.map { i => i })

      val b = StreamBuffer(src, 3)

      val w = StreamTransformer.map(b, q =>
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      )

      val f = StreamSink(w, l.add).start

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3", "w:s:5"), l.asScala)
  }

  @Test
  def zipStream(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = StreamSource(data)

      val w1 = StreamTransformer.map(src, q => {
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      })

      val w2 = StreamTransformer.map(src, q => {
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      })

      val r = ZipStream(w1, w2)

      val f = StreamSink(r, l.add).start

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)

    }

    Assertions.assertEquals(Set("w:s:0", "w:s:1", "w:s:2", "w:s:3", "w:s:5"), l.asScala.toSet)

  }

}
