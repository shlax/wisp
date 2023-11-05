package org.miniActor.test.tutorial

import org.miniActor.ActorSystem
import org.miniActor.stream.iterator.{MapFlow, WaitSourceSink, Source}
import org.scalatest.funsuite.AnyFunSuite
import org.miniActor.stream.iterator.Source.*
import scala.util.Using

class SplitJoinStreamHelloWorld extends  AnyFunSuite {

  test("splitJoinStreamHelloWorld") {
    Using(new ActorSystem) { system =>
      val range:Source[Int] = (1 to 10).iterator.asSource

      val source = WaitSourceSink[Int](() => {
        val e = range.next()
        println(">" + Thread.currentThread() + ":" + e)
        e
      })({ v =>
        println("<" + Thread.currentThread() + ":" + v)
      })

      val flow = system.create(c => MapFlow(source, c)({
        case i: Int =>
          Thread.sleep(50)
          "" + Thread.currentThread() + ">" + i
      }))

      source.run(flow) //  will block current until all elements are not processed

    }.get

  }

}
