package org.wisp.test.tutorial

import org.wisp.ActorSystem
import org.wisp.stream.iterator.{MapFlow, StreamSink, StreamSource, ForEachSource, WaitSink}
import org.scalatest.funsuite.AnyFunSuite
import org.wisp.stream.iterator.Source.*
import scala.util.Using

class WaitActorStreamHelloWorld extends  AnyFunSuite {

  test("waitActorStreamHelloWorld") {
    Using(new ActorSystem) { system =>
      val range = (1 to 10).iterator

      val source = StreamSource(system, range.asSource) // Iterator will be called from multiple threads
      val flow = system.create(c => MapFlow(source, c)({
        case i: Int => "" + Thread.currentThread() + ">" + i
      }))
      val sink = WaitSink(flow){ i =>
        println("" + Thread.currentThread() + ":" + i)
      }

      sink.run() //  will block current until all elements are not send

    }.get
  }

}
