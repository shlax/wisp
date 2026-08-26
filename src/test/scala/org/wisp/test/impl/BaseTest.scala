package org.wisp.test.impl

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.exceptions.UndeliveredException
import org.wisp.stream.SinkSource
import org.wisp.stream.extensions.asSource
import org.wisp.{Link, Message}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class BaseTest {

  @Test
  def message():Unit = {
    var res = 0

    val m:Message[Int, Int] = Message(2, new Link[Int, Int] {
      override def apply(t: Message[Int, Int]): Unit = {
        res += t.value
      }
    })

    m match {
      case Message(a, b) =>
        b << a + 1
    }

    Assertions.assertEquals(3, res)

  }

  @Test
  def undeliveredException(): Unit = {
    val l:Link[Int, Int] = new Link[Int, Int] {
      override def apply(t: Message[Int, Int]): Unit = {
        t.sender << t.value + 1
      }
    }

    var m:Option[Message[?, ?]] = None

    try {
      l << 2
    }catch {
      case UndeliveredException(msg) =>
        m = Some(msg)
    }

    Assertions.assertEquals(m.get.value, 3)

  }

  @Test
  def sinkSource():Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val s = SinkSource[Int]()

    val f1 = Future[List[Int]] {
      var l:List[Int] = Nil
      s.source.forEach{ i =>
        l = i :: l
      }
      l
    }

    val f2 = Future[Unit] {
      val x = s.sink
      x.apply(1)
      x.apply(2)
      x.complete()
    }

    Await.ready(f1, 1.second)
    Await.ready(f2, 1.second)

    Assertions.assertEquals(f1.value.get.get, List(2,1))

  }

  @Test
  def sinkSourceConsume():Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val s = SinkSource[Int]()

    val f1 = Future[List[Int]] {
      var l:List[Int] = Nil
      s.source.forEach{ i =>
        l = i :: l
      }
      l
    }

    val f2 = Future[Unit] {
      s.sink.consume( Seq(1,2).asSource )
    }

    Await.ready(f1, 1.second)
    Await.ready(f2, 1.second)

    Assertions.assertEquals(f1.value.get.get, List(2,1))

  }


}
