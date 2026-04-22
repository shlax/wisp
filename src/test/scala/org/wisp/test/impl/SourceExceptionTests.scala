package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{Link, ActorSystem}
import org.wisp.stream.Sink
import tests.*
import org.wisp.stream.extensions.*
import org.wisp.stream.iterator.{RunnableSourceSink, RunnableSource, RunnableSink, StreamBuffer, StreamSink, StreamSource, StreamWorker, ZipStream}

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Await
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*
import scala.concurrent.duration.*
import scala.util.Failure

class SourceExceptionTests {
  class MyException(msg: String) extends RuntimeException(msg)

  @Test
  def forEachSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    val ar = AtomicReference[Throwable]()

    try {
      ActorSystem() || { sys =>

        val thread = Thread.currentThread()

        val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
          Assertions.assertTrue(Thread.currentThread() == thread)
          if (i == 4) throw new MyException("is 4")
          "s:" + i
        }

        val sink = new Sink[String] {
          override def apply(t: String): Unit = {
            Assertions.assertTrue(Thread.currentThread() == thread)
            l.add(t)
          }
        }

        val src = RunnableSourceSink(data, sink) { ref =>
          StreamWorker.map(ref, (q: String) =>
            "w:" + q
          )
        }

        src.run()

      }
    }catch{
      case NonFatal(e) =>
        ar.set(e)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)
    Assertions.assertTrue(ar.get().isInstanceOf[MyException])
    Assertions.assertEquals(ar.get().getMessage, "is 4")

  }

  @Test
  def forEachSource():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val thread = Thread.currentThread()

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread() == thread)
        if (i == 4) throw new MyException("is 4")
        "s:" + i
      }

      val src = RunnableSource(data)

      val w = StreamWorker.map(src, q =>
        "w:" + q
      )

      val f = StreamSink(w, l.add).start

      var srcEx:Option[Throwable] = None

      try {
        src.run()
      }catch {
        case NonFatal(se) =>
          srcEx = Some(se)
      }

      Assertions.assertTrue(srcEx.get.isInstanceOf[MyException])

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)

  }

  @Test
  def runnableSink():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        if (i == 4) throw new MyException("is 4")
        "s:" + i
      }

      val src = StreamSource(data)

      val w = StreamWorker.map(src, q =>
        "w:" + q
      )

      RunnableSink(w, l.add).run()

    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)

  }

  @Test
  def streamWorker():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        if (i == 4) throw new MyException("is 4")
        "s:" + i
      }

      val src = StreamSource(data)

      val w = StreamWorker.map(src, q =>
        "w:" + q
      )

      val f = StreamSink(w, l.add).start

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)

  }

  @Test
  def streamBuffer():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        if (i == 4) throw new MyException("is 4")
        "s:" + i
      }

      val src = StreamSource(data.map { i => i })

      val b = StreamBuffer(src, 3)

      val w = StreamWorker.map(b, q =>
        "w:" + q
      )

      val f = StreamSink(w, l.add).start

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)

    }

    Assertions.assertTrue(l.asScala.contains("w:s:0"))
  }

  @Test
  def zipStream():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        if (i == 4) throw new MyException("is 4")
        "s:" + i
      }

      val src = StreamSource(data)

      val w1 = StreamWorker.map(src, q => {
        "w:" + q
      })

      val w2 = StreamWorker.map(src, q => {
        "w:" + q
      })

      val r = ZipStream(w1, w2)

      val f = StreamSink(r, l.add).start

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)

    }

  }

}
