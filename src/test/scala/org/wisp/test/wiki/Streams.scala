package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.stream.typed.StreamGraph
import org.wisp.stream.Source.*
import org.wisp.using.*

import scala.concurrent.Await
import scala.concurrent.duration.*

class Streams {

  @Test
  def simpleStream():Unit = {
    // create ActorSystem
    ActorSystem() | { system =>

      // test data
      val data = 1 to 3

      // give system as ExecutionContext
      system.as { sys =>
        // stream graph builder
        val graph = StreamGraph(sys)

        // convert data to Source
        val source = data.asSource

        // create stream from source then add 1 and print result
        val stream = graph.from(source).map(_ + 1).to(println)

        // start execution and wait for completion
        Await.ready(stream.start(), 1.second)
      }

    }

  }

}
