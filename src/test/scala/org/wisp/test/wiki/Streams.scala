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
  def simple():Unit = {
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
  def workers(): Unit = {
    // create ActorSystem
    ActorSystem() | { system =>
      given ExecutionContext = system
      // test data
      val data = 1 to 10
      // create graph builder
      val graph = StreamGraph(system)
      // convert data to Source
      val source = data.asSource

      // create stream
      val stream = graph.from(source).as{ src =>

        // create worker 1
        val w1 = src.map(i => {
            Thread.sleep(10)
            "w1:"+i
          }).to(println) // to sink 1

        // create worker 2
        val w2 = src.map(i => {
            Thread.sleep(20)
            "w2:"+i
          }).to(println) // to sink 2

        Seq(w1, w2)
      }
      // start execution and wait for completion
      Await.ready(Future.sequence(stream.map(_.start())), 1.second)
    }
  }

  @Test
  def zip(): Unit = {
    // create ActorSystem
    ActorSystem() | { system =>
      given ExecutionContext = system

      // test data
      val data = 1 to 10
      // create graph builder
      val graph = StreamGraph(system)
      // convert data to Source
      val source = data.asSource

      // create stream
      val stream = graph.from(source).as { src =>

        // create worker 1
        val w1 = src.map(i => {
          Thread.sleep(10)
          "w1:" + i
        })

        // create worker 2
        val w2 = src.map(i => {
          Thread.sleep(20)
          "w2:" + i
        })

        // zip streams
        graph.zip(Seq(w1, w2)).to(println)
      }
      // start execution and wait for completion
      Await.ready(stream.start(), 1.second)
    }
  }

  @Test
  def split(): Unit = {
    // create ActorSystem
    ActorSystem() | { system =>
      given ExecutionContext = system

      // test data
      val data = 1 to 10
      // create graph builder
      val graph = StreamGraph(system)
      // convert data to Source
      val source = data.asSource

      // create stream
      val stream = graph.from(source).as { src =>

        // duplicate stream so each worker will receive each item
        src.split{ split =>

          // create worker 1
          val w1 = split.next().map(i => {
            Thread.sleep(10)
            "w1:" + i
          }).to(println) // to sink 1

          // create worker 2
          val w2 = split.next().map(i => {
            Thread.sleep(20)
            "w2:" + i
          }).to(println) // to sink 2

          Seq(w1, w2)
        }

      }
      // start execution and wait for completion
      Await.ready(Future.sequence(stream.map(_.start())), 1.second)
    }
  }

}
