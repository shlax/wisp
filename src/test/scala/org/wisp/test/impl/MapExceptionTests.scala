package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.Sink
import testSystem.*
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{ForEachSink, ForEachSource, RunnableSink, StreamBuffer, StreamSink, StreamSource, StreamWorker, ZipStream}

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Await
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*
import scala.concurrent.duration.*
import scala.util.Failure

class MapExceptionTests {
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
            l.add(t)
          }
        }

        val src = ForEachSink(data, sink) { (ref: ActorLink) =>
          sys.create(i => StreamWorker.map(ref, i, (q: String) =>
            if (q == "s:4") throw new MyException("is 4")
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
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      ))

      val f = StreamSink(w, l.add).start()
      
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
          if (q == "s:4") throw new MyException("is 4")
          "w:" + q
        ))

        RunnableSink(w, l.add).run()

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
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      ))

      val f = StreamSink(w, l.add).start()

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
  def streamBuffer(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    val ar = AtomicReference[Throwable]()

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = StreamSource(data.map { i => i })

      val b = StreamBuffer(src, 3)

      val w = sys.create(i => StreamWorker.map(b, i, q =>
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      ))

      val f = StreamSink(w, l.add).start()

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isFailure)
      v match {
        case Failure(q) =>
          ar.set(q)
        case _ =>
      }

    }

    Assertions.assertTrue(l.asScala.contains("w:s:0"))
    Assertions.assertTrue(ar.get().isInstanceOf[MyException])
    Assertions.assertEquals(ar.get().getMessage, "is 4")
  }

  @Test
  def zipStream(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    val ar = AtomicReference[Throwable]()

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        "s:" + i
      }

      val src = StreamSource(data)

      val w1 = sys.create(i => StreamWorker.map(src, i, q => {
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      }))

      val w2 = sys.create(i => StreamWorker.map(src, i, q => {
        if (q == "s:4") throw new MyException("is 4")
        "w:" + q
      }))

      val r = ZipStream(w1, w2)

      val f = StreamSink(r, l.add).start()

      Await.ready(f, 1.second)
      val v = f.value.get
      Assertions.assertTrue(v.isFailure)
      v match {
        case Failure(q) =>
          ar.set(q)
        case _ =>
      }

    }

    Assertions.assertTrue(ar.get().isInstanceOf[MyException])
    Assertions.assertEquals(ar.get().getMessage, "is 4")

  }

}
