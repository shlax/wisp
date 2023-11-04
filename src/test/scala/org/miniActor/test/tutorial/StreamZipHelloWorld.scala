package org.miniActor.test.tutorial

import org.miniActor.ActorSystem
import org.miniActor.stream.iterator.{MapFlow, StreamSink, StreamSource, ZipStream}
import org.scalatest.funsuite.AnyFunSuite
import org.miniActor.stream.iterator.Source.*
import scala.util.Using

class StreamZipHelloWorld extends AnyFunSuite {

  test("streamZipHelloWorld") {
    Using(new ActorSystem) { system =>
      val range = (1 to 10).iterator

      val source = StreamSource(range.asSource) // Iterator will be called from multiple threads
      val flow1 = system.create(c => MapFlow(source, c)({
        case i: Int =>
          println("1 << "+i)
          Thread.sleep(250)
          " < 1: " + Thread.currentThread() + ">" + i
      }))
      val flow2 = system.create(c => MapFlow(source, c)({
        case i: Int =>
          println("2 << "+i)
          Thread.sleep(250)
          " < 2: " + Thread.currentThread() + ">" + i
      }))
      val zip = ZipStream(Seq(flow1, flow2))
      val sink = StreamSink(zip){ r =>
        println(""+Thread.currentThread()+r)
      }

      sink.start().get() // start processing data
    }.get
  }

}
