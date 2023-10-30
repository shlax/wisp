package org.qwActor.test.tutorial

import org.qwActor.ActorSystem
import org.qwActor.stream.iterator.{ActorSink, ActorSource, SplitActor}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Using

class SplitHelloWorld extends AnyFunSuite{

  test("splitHelloWorld"){
    Using(new ActorSystem) { system =>
      val range = (1 to 10).iterator

      val source = ActorSource[Int](range) // Iterator will be called from multiple threads
      val split = SplitActor.apply(source)

      val sink1 = ActorSink(split.add()) { r =>
        println("1:" + r)
      }

      val sink2 = ActorSink(split.add()) { r =>
        println("2:" + r)
      }

      val c1 = sink1.start()
      val c2 = sink2.start()

      c1.get()
      c2.get()

    }.get
  }

}
