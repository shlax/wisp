package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.remote.{RemoteLink, UdpClient, UdpRouter}
import org.wisp.stream.Sink
import org.wisp.stream.extensions.*
import org.wisp.stream.iterator.{RunnableSourceSink, RunnableSource, RunnableSink, SplitStream, StreamBuffer, StreamSink, StreamSource, StreamWorker, ZipStream}
import org.wisp.stream.typed.StreamGraph
import tests.*
import org.wisp.closeable.*
import org.wisp.{AbstractActor, ActorLink, ActorSystem, ActorScheduler}

import java.net.InetSocketAddress
import java.util
import java.util.Collections
import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration.*
import scala.util.Success
import scala.jdk.CollectionConverters.*

class BasicTests {

  @Test
  def helloWorld(): Unit = {
    val cd = new CountDownLatch(1)
    val ref = AtomicReference[Any]()

    class HelloActor(in: ActorScheduler) extends AbstractActor(in) {
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a =>
          ref.set(a)
          cd.countDown()
      }
    }

    new ActorSystem()||{ sys =>
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

    class HelloActor(in: ActorScheduler) extends AbstractActor(in) {
      override def accept(from: ActorLink): PartialFunction[Any, Unit] = {
        case a => from << "Hello " + a
      }
    }

    ActorSystem() || { sys =>

      val hello = sys.create(HelloActor(_))
      hello.call("world").onComplete { e =>
        ref.set(e.get.value)
        cd.countDown()
      }

      cd.await()
    }

    Assertions.assertEquals("Hello world", ref.get())
  }

  @Test
  def sinkFlatMap():Unit = {
    val data = Seq(List(0,1,2),List(3,4,5)).asSource
    val l = ArrayBuffer[Int]()

    val x = Sink[Int](i => l += i)
    val y = x.flatMap[List[Int]]{ (t, self) =>
      for (i <- t) self.accept(i)
    }

    y.consume(data)
    Assertions.assertEquals(0 to 5, l)
  }

  @Test
  def sinkFold(): Unit = {
    val data = Seq(1, 2, 3).asSource
    
    val (s, f) = Promise[Int]().asSink[Int](0){ (a, b) => a + b }
    Assertions.assertFalse(f.isCompleted)
    s.consume(data)

    Assertions.assertEquals(Some(Success(6)), f.value)
  }

  @Test
  def sinkFoldFilter(): Unit = {
    val data = Seq(1, 2, 3).asSource
    
    val (s, p) = Promise[Int]().asSink[Int](0){ (a, b) => a + b }
    val f = s.filter( _ % 2 == 1 )
    Assertions.assertFalse(p.isCompleted)
    f.consume(data)

    Assertions.assertEquals(Some(Success(4)), p.value)
  }

  @Test
  def sinkToMap(): Unit = {
    val data = Seq(1, 2, 3).asSource

    var r:Option[String] = None

    val s1 = Sink[String]{ i => r = Some(i) }
    val s2 = s1.map( (i:Int) => ""+i )
    s2.accept(1)

    Assertions.assertEquals("1", r.get)
  }

  @Test
  def sinkSimple(): Unit = {
    val data = Seq(1, 2, 3).asSource

    val l = ArrayBuffer[Int]()

    val t = Sink[Int](i => l += i).map[Int](_ + 1)

    t.consume(data)

    Assertions.assertEquals(List(2, 3, 4), l.toList)
  }

  @Test
  def sourceFlatMap():Unit = {
    val data = Seq(List(0, 1, 2), List(3, 4, 5)).asSource
    val l = ArrayBuffer[Int]()

    data.flatMap( i => i.asSource ).forEach(i => l += i)

    Assertions.assertEquals(0 to 5, l)
  }

  @Test
  def sourceFold(): Unit = {
    val data = Seq(1, 2, 3).asSource

    val r = data.fold(0)((a, b) => a + b)

    Assertions.assertEquals(6, r)
  }

  @Test
  def typedStreamGraph():Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource
    val l = Collections.synchronizedList(new util.ArrayList[Int]())

    ActorSystem() || { sys =>
      val p = StreamGraph(sys).from(data).map(i => i + 1).to(l.add).start()
      Await.result(p, 1.second)
    }

    Assertions.assertEquals(1 to 6, l.asScala)

  }

  @Test
  def typedSinkTree():Unit = {
    val data = Seq(0, 1, 2, 3, 4, 5).asSource

    val l1 = Collections.synchronizedList(new util.ArrayList[String]())
    val l2 = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val s1 = Sink[String](l1.add).map[Int]("a:" + _).map[Int](i => i * 2 + 0)
      val s2 = Sink[String](l2.add).map[Int]("b:" + _).map[Int](i => i * 2 + 1)
      val t = s1.thenTo(s2)

      val p = StreamGraph(sys).from(data).map(i => i + 1).to(t).start()
      Await.result(p, 1.second)
    }

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
      r.register("echo", s.create(i => new AbstractActor(i) {
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
  def forEachSink():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val thread = Thread.currentThread()

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
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

    Assertions.assertEquals(List("d:w:s:0", "d:w:s:1", "d:w:s:2", "d:w:s:3", "d:w:s:4", "d:w:s:5"), l.asScala)

  }

  @Test
  def forEachSource():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val tId = Thread.currentThread()

      val data = Seq(0, 1, 2, 3, 4, 5).asSource.map { i =>
        Assertions.assertTrue(Thread.currentThread() == tId)
        "s:" + i
      }
      val src = RunnableSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      src.run()
      Await.ready(p, 1.second)

    }

    Assertions.assertEquals(List("w:s:0", "w:s:1", "w:s:2", "w:s:3", "w:s:4", "w:s:5"), l.asScala)

  }

  @Test
  def runnableSink():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource

      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      RunnableSink(w, l.add).run()

    }

    Assertions.assertEquals(List("w:0", "w:1", "w:2", "w:3", "w:4", "w:5"), l.asScala)

  }

  @Test
  def streamBuffer():Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource

      val src = StreamSource(data.map { i => i })

      val b = StreamBuffer(src, 3)

      val w = sys.create(i => StreamWorker.map(b, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      Await.ready(p, 1.second)
    }

    Assertions.assertEquals(List("w:0", "w:1", "w:2", "w:3", "w:4", "w:5"), l.asScala)

  }

  @Test
  def streamWorker(): Unit = {
    val l = Collections.synchronizedList(new util.ArrayList[String]())
    
    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4, 5).asSource
      val src = StreamSource(data)

      val w = sys.create(i => StreamWorker.map(src, i, q =>
        "w:" + q
      ))

      val p = StreamSink(w, l.add).start()
      Await.ready(p, 1.second)

    }

    Assertions.assertEquals(List("w:0", "w:1", "w:2", "w:3", "w:4", "w:5"), l.asScala)
    
  }

  @Test
  def zipStream():Unit = {
    val l = Collections.synchronizedSet(new util.HashSet[String]())
    
    ActorSystem() || { sys =>

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
      Await.ready(p, 1.second)

    }

    Assertions.assertEquals(Set("w:0", "w:1", "w:2", "w:3", "w:4"), l.asScala)
    
  }


  @Test
  def splitStream():Unit = {
    val l1 = Collections.synchronizedList(new util.ArrayList[Int]())
    val l2 = Collections.synchronizedList(new util.ArrayList[Int]())

    ActorSystem() || { sys =>

      val data = Seq(0, 1, 2, 3, 4).asSource
      val src = StreamSource(data)

      var sl:List[StreamSink[?]] = Nil

      val r = SplitStream(src){ b =>
        sl = StreamSink( b.next(), l1.add ) :: sl
        sl = StreamSink( b.next(), l2.add ) :: sl
      }

      val p = Future.sequence( sl.map(_.start()) )
      Await.ready(p, 1.second)

    }

    Assertions.assertEquals(List(0, 1, 2, 3, 4), l1.asScala.toList)
    Assertions.assertEquals(List(0, 1, 2, 3, 4), l2.asScala.toList)

  }

  @Test
  def typedSplitStream(): Unit = {
    val data = Seq(0, 1, 2, 3, 4).asSource

    val l1 = Collections.synchronizedList(new util.ArrayList[Int]())
    val l2 = Collections.synchronizedList(new util.ArrayList[Int]())

    ActorSystem() || { sys =>

      val p = StreamGraph(sys).from(data).split{ n =>
        Seq( n.next().to(l1.add), n.next().to(l2.add) )
      }

      Await.result( Future.sequence( p.map(_.start()) ), 1.second)
    }

    Assertions.assertEquals(List(0, 1, 2, 3, 4), l1.asScala.toList)
    Assertions.assertEquals(List(0, 1, 2, 3, 4), l2.asScala.toList)

  }

}
