package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.typed.StreamGraph
import org.wisp.stream.Source.*
import org.wisp.using.*

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

class Streams {

  @Test
  def simpleStream():Unit = {
    // create ActorSystem
    ActorSystem() | { system =>
      // test data
      val data = 1 to 3
      // create graph builder
      val graph = StreamGraph(system)
      // convert data to Source
      val source = data.asSource
      // create stream from source then add 1 and print result
      val stream = graph.from(source).map(_ + 1).to(println)
      // start execution and wait for completion
      Await.ready(stream.start(), 1.second)
    }
  }

  @Test
  def workersStream(): Unit = {
    // create ActorSystem
    ActorSystem() | { system =>
      given ExecutionContext = system
      // test data
      val data = 1 to 10
      // create graph builder
      val graph = StreamGraph(system)
      // convert data to Source
      val source = data.asSource
      // create stream from source
      val stream = graph.from(source).as{ src =>
        // create first worker
        val w1 = src.map(i => {
            Thread.sleep(10)
            "w1:"+i
          }).to(println)
        // create second worker
        val w2 = src.map(i => {
            Thread.sleep(20)
            "w2:"+i
          }).to(println)

        List(w1, w2)
      }
      // start execution and wait for completion
      Await.ready(Future.sequence(stream.map(_.start())), 1.second)
    }
  }

}
