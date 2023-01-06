package org.qwActor.test.tutorial

import org.qwActor.ActorSystem
import org.qwActor.stream.iterator.{ActorFlow, WaitActorSourceSink, Source}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Using

class SplitJoinStreamHelloWorld extends  AnyFunSuite {

  test("splitJoinStreamHelloWorld") {
    Using(new ActorSystem) { system =>
      val range:Source[Int] = (1 to 10).iterator

      val source = WaitActorSourceSink[Int](() => {
        val e = range.next()
        println(">" + Thread.currentThread() + ":" + e)
        e
      })({ v =>
        println("<" + Thread.currentThread() + ":" + v)
      })

      val flow = system.create(c => ActorFlow[String](source, c)({
        case i: Int =>
          Thread.sleep(50)
          "" + Thread.currentThread() + ">" + i
      }))

      source.run(flow) //  will block current until all elements are not processed

    }.get

  }

}
