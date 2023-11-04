package org.miniActor.test.tutorial

import org.miniActor.ActorSystem
import org.miniActor.stream.ForEach
import org.miniActor.stream.iterator.{MapFlow, StreamSink, ForEachSource, Source}
import org.scalatest.funsuite.AnyFunSuite

import java.util.function.Consumer
import scala.util.Using

class BlockingActorStreamHelloWorld extends  AnyFunSuite {

  test("blockingActorStreamHelloWorld") {
    Using(new ActorSystem) { system =>
      val range = (1 to 10).iterator

      val source = ForEachSource[Int](range) // Iterator will be called from current thread
      val flow = system.create(c => MapFlow(source, c)({
        case i : Int => "\t"+Thread.currentThread()+">"+i
      }))
      val sink = StreamSink(flow)(println)

      println("start: "+Thread.currentThread())

      val cf = sink.start() // start pulling data
      source.run() // iterate over range, will block current until all elements are not send
      cf.get() // wait for all messages to propagate

    }.get
  }

}
