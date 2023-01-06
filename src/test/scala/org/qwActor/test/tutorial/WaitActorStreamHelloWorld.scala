package org.qwActor.test.tutorial

import org.qwActor.ActorSystem
import org.qwActor.stream.iterator.{ActorFlow, ActorSink, ActorSource, ForEachSource, WaitActorSink}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Using

class WaitActorStreamHelloWorld extends  AnyFunSuite {

  test("waitActorStreamHelloWorld") {
    Using(new ActorSystem) { system =>
      val range = (1 to 10).iterator

      val source = ActorSource[Int](range) // Iterator will be called from multiple threads
      val flow = system.create(c => ActorFlow[String](source, c)({
        case i: Int => "" + Thread.currentThread() + ">" + i
      }))
      val sink = WaitActorSink(flow){ i =>
        println("" + Thread.currentThread() + ":" + i)
      }

      sink.run() //  will block current until all elements are not send

    }.get
  }

}
