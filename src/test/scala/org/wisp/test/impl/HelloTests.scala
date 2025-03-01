package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.stream.{Sink, SinkTree}
import org.wisp.stream.Source.*
import org.wisp.using.*
import org.wisp.{Actor, ActorLink, ActorSystem, Inbox}

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Promise
import scala.util.Success

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

}
