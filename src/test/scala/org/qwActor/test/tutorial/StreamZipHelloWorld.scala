package org.qwActor.test.tutorial

import org.qwActor.ActorSystem
import org.qwActor.stream.iterator.{ActorFlow, ActorSink, ActorSource, ZipActorFlow}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Using

class StreamZipHelloWorld extends AnyFunSuite {

  test("streamZipHelloWorld") {
    Using(new ActorSystem) { system =>
      val range = (1 to 10).iterator

      val source = ActorSource[Int](range) // Iterator will be called from multiple threads
      val flow1 = system.create(c => ActorFlow[String](source, c)({
        case i: Int =>
          println("1 << "+i)
          Thread.sleep(250)
          " < 1: " + Thread.currentThread() + ">" + i
      }))
      val flow2 = system.create(c => ActorFlow[String](source, c)({
        case i: Int =>
          println("2 << "+i)
          Thread.sleep(250)
          " < 2: " + Thread.currentThread() + ">" + i
      }))
      val zip = system.create(c => ZipActorFlow(Seq(flow1, flow2), c))
      val sink = ActorSink(zip){ r =>
        println(""+Thread.currentThread()+r)
      }

      sink.start().get() // start processing data
    }.get
  }

}
