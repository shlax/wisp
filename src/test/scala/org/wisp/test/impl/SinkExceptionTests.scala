package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.Sink
import testSystem.*
import org.wisp.stream.extensions.*
import org.wisp.stream.iterator.{ForEachSourceSink, ForEachSource, ForEachSink, StreamBuffer, StreamSink, StreamSource, StreamWorker, ZipStream}

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Await
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*
import scala.concurrent.duration.*
import scala.util.Failure

class SinkExceptionTests {
  class MyException(msg: String) extends RuntimeException(msg)

  @Test
  def forEachSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    val ar = AtomicReference[Throwable]()

    try {
      ActorSystem() || { sys =>

        val tId = Thread.currentThread().threadId

        val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
          Assertions.assertTrue(Thread.currentThread().threadId == tId)
          "s:" + i
        }

        val sink = new Sink[String] {
          override def accept(t: String): Unit = {
            Assertions.assertTrue(Thread.currentThread().threadId == tId)
            if (t == "w:s:4") throw new MyException("is 4")
            l.add(t)
          }
        }

        val src = ForEachSourceSink(data, sink) { (ref: ActorLink) =>
          sys.create(i => StreamWorker.map(ref, i, (q: String) =>
            "w:" + q
          ))
        }

        src.run()

      }
    } catch {
      case NonFatal(e) =>
        ar.set(e)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)
    Assertions.assertTrue(ar.get().isInstanceOf[MyException])
    Assertions.assertEquals(ar.get().getMessage, "is 4")

  }

  @Test
  def forEachSource(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    val ar = AtomicReference[Throwable]()

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = ForEachSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val f = StreamSink(w, (q:String) => {
        if (q == "w:s:4") throw new MyException("is 4")
        l.add(q)
      }).start()
      
      src.failOn(f).run()

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isFailure)
      v match {
        case Failure(q) =>
          ar.set(q)
        case _ =>
      }
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)
    Assertions.assertTrue(ar.get().isInstanceOf[MyException])
    Assertions.assertEquals(ar.get().getMessage, "is 4")

  }

  @Test
  def runnableSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    val ar = AtomicReference[Throwable]()

    try {
      ActorSystem() || { sys =>

        val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
          "s:" + i
        }

        val src = StreamSource(data)

        val w = sys.create(i => StreamWorker.map(src, i, q =>
          "w:" + q
        ))

        ForEachSink(w, (q:String) => {
          if (q == "w:s:4") throw new MyException("is 4")
          l.add(q)
        }).run()

      }
    } catch {
      case NonFatal(e) =>
        ar.set(e)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)
    Assertions.assertTrue(ar.get().isInstanceOf[MyException])
    Assertions.assertEquals(ar.get().getMessage, "is 4")

  }

  @Test
  def streamWorker(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    val ar = AtomicReference[Throwable]()

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val f = StreamSink(w, (q:String) =>{
        if (q == "w:s:4") throw new MyException("is 4")
        l.add(q)
      }).start()

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isFailure)
      v match {
        case Failure(q) =>
          ar.set(q)
        case _ =>
      }
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)
    Assertions.assertTrue(ar.get().isInstanceOf[MyException])
    Assertions.assertEquals(ar.get().getMessage, "is 4")

  }
}
