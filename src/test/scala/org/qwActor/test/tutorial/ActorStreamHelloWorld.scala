package org.qwActor.test.tutorial

import org.qwActor.ActorSystem
import org.qwActor.stream.iterator.{MapFlow, StreamSink, StreamSource, ForEachSource}
import org.scalatest.funsuite.AnyFunSuite
import org.qwActor.stream.iterator.Source.*

import scala.util.Using

class ActorStreamHelloWorld extends AnyFunSuite {

  test("actorStreamHelloWorld") {
    Using(new ActorSystem) { system =>
      val range = (1 to 10).iterator

      val source = StreamSource(range.asSource) // Iterator will be called from multiple threads
      val flow = system.create(c => MapFlow(source, c)({
        case i : Int => ""+Thread.currentThread()+">"+i
      }))
      val sink = StreamSink(flow)(println)

      sink.start().get() // start processing data
    }.get
  }


}
