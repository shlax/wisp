package org.miniActor.test.tutorial

import org.miniActor.{Actor, ActorSystem}
import org.miniActor.stream.iterator.{FlatMapFlow, StreamSink, StreamSource, WaitSink}
import org.scalatest.funsuite.AnyFunSuite
import org.miniActor.stream.iterator.Source.*
import scala.util.Using

class FlatMapStreamHelloWorld extends AnyFunSuite{

  test("flatMapStreamHelloWorld") {
    Using(new ActorSystem) { system =>
      val in = Seq("", "a", "bc", "", "def", "")

      val source = StreamSource(in.asSource)

      val flow = system.create( c => FlatMapFlow(source, c) { e =>
        val arr = e.toString.toCharArray.map(_.toString)
        arr.asSource
      })

      val sink = StreamSink(flow) { i =>
        println("" + Thread.currentThread() + ":" + i)
      }

      sink.start().get()
    }.get

  }

  test("flatMapStreamHelloWorldWait") {
    Using(new ActorSystem) { system =>

      val in = Seq("", "a", "bc", "", "def", "")

      val source = StreamSource(in.asSource)

      val flow = system.create(c => FlatMapFlow(source, c) { e =>
        val arr = e.toString.toCharArray.map(_.toString)
        arr.asSource
      })

      val sink = WaitSink(flow) { i =>
        println("" + Thread.currentThread() + ":" + i)
      }

      sink.run()

    }.get

  }

}