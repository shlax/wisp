package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{Link, ActorSystem}
import org.wisp.stream.Sink
import org.wisp.utils.extensions.*
import org.wisp.stream.extensions.*
import org.wisp.stream.iterator.{RunnableSourceSink, RunnableSource, RunnableSink, StreamSink, StreamSource, StreamWorker}

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Await
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*
import scala.concurrent.duration.*
import scala.util.Failure

class FlatMapExceptionTests {
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
        StreamWorker.flatMap(ref, (q: String) =>
          if (q == "s:3"){
            List("w:x:3", "w:x:4").asSource.map{ q =>
              if(q == "w:x:4") {
                throw new MyException("is 4")
              }
              q
            }
          }else List("w:" + q).asSource
        )
      }

      src.run()

    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:x:3", "w:s:4", "w:s:5"), l.asScala)

  }

  @Test
  def forEachSource(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = RunnableSource(data)

      val w = StreamWorker.flatMap(src, (q: String) =>
        if (q == "s:4"){
          List("w:s:4").asSource.map{ q =>
            if(q == "w:s:4") {
              throw new MyException("is 4")
            }
            q
          }
        }else List("w:" + q).asSource
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

      val w = StreamWorker.flatMap(src, (q: String) =>
        if (q == "s:4"){
          List("w:s:4").asSource.map{ q =>
            if(q == "w:s:4") {
              throw new MyException("is 4")
            }
            q
          }
        }else List("w:" + q).asSource
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

      val w = StreamWorker.flatMap(src, (q: String) =>
        if (q == "s:3"){
          List("w:x:3", "w:x:4").asSource.map{ q =>
            if(q == "w:x:4") {
              throw new MyException("is 4")
            }
            q
          }
        }else List("w:" + q).asSource
      )

      val f = StreamSink(w, l.add).start

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isSuccess)

    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:x:3", "w:s:4", "w:s:5"), l.asScala)

  }

}
