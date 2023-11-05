package org.wisp.test.tutorial

import org.wisp.ActorSystem
import org.wisp.stream.iterator.{StreamSink, StreamSource, SplitStream}
import org.scalatest.funsuite.AnyFunSuite
import org.wisp.stream.iterator.Source.*
import scala.util.Using

class SplitHelloWorld extends AnyFunSuite{

  test("splitHelloWorld"){
    val range = (1 to 10).iterator

    val source = StreamSource(range.asSource) // Iterator will be called from multiple threads
    val split = SplitStream.apply(source)

    val sink1 = StreamSink(split.add()) { r =>
      println("1:" + r)
    }

    val sink2 = StreamSink(split.add()) { r =>
      println("2:" + r)
    }

    val c1 = sink1.start()
    val c2 = sink2.start()

    c1.get()
    c2.get()
  }

}
