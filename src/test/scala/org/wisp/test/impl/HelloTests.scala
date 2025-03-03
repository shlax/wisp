package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.remote.{RemoteLink, UdpClient, UdpRouter}
import org.wisp.stream.{Sink, SinkTree}
import org.wisp.stream.Source.*
import org.wisp.stream.iterator.{ForEachSink, ForEachSource, RunnableSink, StreamBuffer, StreamSink, StreamSource, StreamWorker, ZipStream}
import org.wisp.stream.typed.StreamGraph
import org.wisp.using.*
import org.wisp.{Actor, ActorLink, ActorSystem, Inbox}

import java.net.InetSocketAddress
import java.util
import java.util.Collections
import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.*
import scala.util.Success
import scala.jdk.CollectionConverters.*

class HelloTests {

  @Test
  def helloWorld(): Unit = {
    val cd = new CountDownLatch(1)
    val ref = AtomicReference[Any]()

    class HelloActor(in: Inbox) extends Actor(in) {
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a =>
          ref.set(a)
          cd.countDown()
      }
    }

    new ActorSystem()|{ sys =>
      val hello = sys.create(HelloActor(_))
      hello << "Hello world"

      cd.await()
    }

    Assertions.assertEquals("Hello world", ref.get())
  }

  @Test
  def helloAsk():Unit = {
    val cd = new CountDownLatch(1)
    val ref = AtomicReference[Any]()

    class HelloActor(in: Inbox) extends Actor(in) {
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a => from << "Hello " + a
      }
    }

    ActorSystem() | ( _.as { sys =>

      val hello = sys.create(HelloActor(_))
      hello.ask("world").future.onComplete { e =>
        ref.set(e.get.value)
        cd.countDown()
      }

      cd.await()
    })

    Assertions.assertEquals("Hello world", ref.get())
  }

  @Test
  def helloSinkFlatMap():Unit = {
    val data = Seq(List(0,1,2),List(3,4,5)).asSource
    val l = ArrayBuffer[Int]()

    SinkTree(data){ f =>
      f.flatMap{ (x:Sink[Int]) =>
        (t: List[Int]) => for (i <- t) x.accept(i)
      }.map(i => l += i)
    }

    Assertions.assertEquals(0 to 5, l)
  }

  @Test
  def helloSinkFold(): Unit = {
    val data = Seq(1, 2, 3).asSource

    val p:Promise[Int] = SinkTree(data){ f =>
      val tmp = f.fold(0)( (a, b) => a + b)
      Assertions.assertFalse(tmp.isCompleted)
      tmp
    }

    Assertions.assertEquals(Some(Success(6)), p.future.value)
  }

  @Test
  def helloSourceFlatMap():Unit = {
    val data = Seq(List(0, 1, 2), List(3, 4, 5)).asSource
    val l = ArrayBuffer[Int]()

    data.flatMap( i => i.asSource ).forEach(i => l += i)

    Assertions.assertEquals(0 to 5, l)
  }

  @Test
  def helloSourceFold(): Unit = {
    val data = Seq(1, 2, 3).asSource

    val r = data.fold(0)((a, b) => a + b)

    Assertions.assertEquals(6, r)
  }

  @Test
  def helloTypedStreamGraph():Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource
    val l = Collections.synchronizedList(new util.ArrayList[Int]())

    ActorSystem() | (_.as { sys =>
      val p = StreamGraph(sys).from(data).map(i => i + 1).to(l.add).start()
      Await.result(p.future, 1.second)
    })

    Assertions.assertEquals(1 to 6, l.asScala)

  }

  @Test
  def helloTypedSinkTree():Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource

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

    Assertions.assertEquals(List("a:2", "a:4", "a:6", "a:8", "a:10", "a:12"), l1.asScala)
    Assertions.assertEquals(List("b:3", "b:5", "b:7", "b:9", "b:11", "b:13"), l2.asScala)

  }

  @Test
  def helloRemote():Unit = {
    val cd = CountDownLatch(2)
    val adr = InetSocketAddress("localhost", 9845)

    val res = Collections.synchronizedSet(new util.HashSet[Any]())

    using { use =>
      val s = use(ActorSystem())

      val r = use(UdpRouter(adr, 2024)(using s))
      r.register("echo", s.create(i => new Actor(i) {
        override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
          case x: Any =>
            res.add(x)
            cd.countDown()
        }
      }))
      s.execute(r)

      val c = use(UdpClient())
      val l = RemoteLink(c, adr, "echo")

      l << "ab"
      l << "cd"

      cd.await(3, TimeUnit.SECONDS)
    }

    Assertions.assertEquals(Set("ab", "cd"), res.asScala)

  }

  @Test
  def helloForEachSink():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val tId = Thread.currentThread().threadId

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
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

    Assertions.assertEquals(List("d:w:s:0", "d:w:s:1", "d:w:s:2", "d:w:s:3", "d:w:s:4", "d:w:s:5"), l.asScala)

  }

  @Test
  def helloForEachSource():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val tId = Thread.currentThread().threadId

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread().threadId == tId)
        "s:" + i
      }
      val src = ForEachSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      src.run()
      Await.ready(p.future, 1.second)

    })

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3", "w:s:4", "w:s:5"), l.asScala)

  }

  @Test
  def helloRunnableSink():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource

      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      RunnableSink(w, l.add).run()

    })

    Assertions.assertEquals(List("w:0", "w:1", "w:2", "w:3", "w:4", "w:5"), l.asScala)

  }

  @Test
  def helloStreamBuffer():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() | (_.as { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource

      val src = StreamSource(data.map { i => i })

      val b = StreamBuffer(src, 3)

      val w = sys.create(i => StreamWorker.map(b, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      Await.ready(p.future, 1.second)
    })

    Assertions.assertEquals(List("w:0", "w:1", "w:2", "w:3", "w:4", "w:5"), l.asScala)

  }

  @Test
  def helloStreamWorker(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    
    ActorSystem() | (_.as { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource
      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      Await.ready(p.future, 1.second)

    })

    Assertions.assertEquals(List("w:0", "w:1", "w:2", "w:3", "w:4", "w:5"), l.asScala)
    
  }

  @Test
  def helloZipStream():Unit = {
    val l = Collections.synchronizedSet(new util.HashSet[String]())
    
    ActorSystem() | (_.as { sys =>

      val data = Seq(0, 1, 2, 3, 4).asSource
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

    Assertions.assertEquals(Set("w:0", "w:1", "w:2", "w:3", "w:4"), l.asScala)
    
  }
  
}
