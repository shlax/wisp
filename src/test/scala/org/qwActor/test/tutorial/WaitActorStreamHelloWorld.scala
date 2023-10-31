package org.qwActor.test.tutorial

import org.qwActor.ActorSystem
import org.qwActor.stream.iterator.{StreamFlow, StreamSink, StreamSource, ForEachSource, WaitSink}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Using

class WaitActorStreamHelloWorld extends  AnyFunSuite {

  test("waitActorStreamHelloWorld") {
    Using(new ActorSystem) { system =>
      val range = (1 to 10).iterator

      val source = StreamSource[Int](range) // Iterator will be called from multiple threads
      val flow = system.create(c => StreamFlow[String](source, c)({
        case i: Int => "" + Thread.currentThread() + ">" + i
      }))
      val sink = WaitSink(flow){ i =>
        println("" + Thread.currentThread() + ":" + i)
      }

      sink.run() //  will block current until all elements are not send

    }.get
  }

}
