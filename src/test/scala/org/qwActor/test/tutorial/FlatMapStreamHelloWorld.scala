package org.qwActor.test.tutorial

import org.qwActor.{Actor, ActorSystem}
import org.qwActor.stream.iterator.{FlatMapStream, StreamSink, StreamSource, WaitSink}
import org.scalatest.funsuite.AnyFunSuite
import org.qwActor.stream.iterator.Source.*
import scala.util.Using

class FlatMapStreamHelloWorld extends AnyFunSuite{

  test("flatMapStreamHelloWorld") {
    val in = Seq("", "a", "bc", "", "def", "")

    val source = StreamSource(in.asSource)

    val flow = FlatMapStream(source){ e =>
      val arr = e.toString.toCharArray.map(_.toString)
      arr.asSource
    }

    val sink = StreamSink(flow) { i =>
      println("" + Thread.currentThread() + ":" + i)
    }

    sink.start().get()

  }

  test("flatMapStreamHelloWorldWait") {
    Using(new ActorSystem) { system =>

      val in = Seq("", "a", "bc", "", "def", "")

      val source = StreamSource(in.asSource)

      val flow = FlatMapStream(source) { e =>
        val arr = e.toString.toCharArray.map(_.toString)
        arr.asSource
      }

      val actor = system.create(c => Actor(flow, c))

      val sink = WaitSink(actor) { i =>
        println("" + Thread.currentThread() + ":" + i)
      }

      sink.run()

    }.get

  }

}
