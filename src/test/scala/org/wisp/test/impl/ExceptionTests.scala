package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.Sink
import org.wisp.using.*
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{ForEachSink, StreamWorker}

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters.*

class ExceptionTests {
  class MyException(msg: String) extends RuntimeException(msg)

  @Test
  def forEachSink(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    val ar = AtomicReference[Throwable]()

    try {
      ActorSystem() | (_.as { sys =>

        val tId = Thread.currentThread().threadId

        val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
          Assertions.assertTrue(Thread.currentThread().threadId == tId)
          if (i == 4) {
            throw new MyException("is 4")
          }
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
            "w:" + q
          ))
        }

        src.run()

      })
    }catch{
      case NonFatal(e) =>
        ar.set(e)
    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3"), l.asScala)
    Assertions.assertTrue(ar.get().isInstanceOf[MyException])
    Assertions.assertEquals(ar.get().getMessage, "is 4")

  }

}
