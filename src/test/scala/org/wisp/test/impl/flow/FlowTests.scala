package org.wisp.test.impl.flow

import org.junit.jupiter.api.{Assertions, Test}
import org.wisp.ActorSystem
import org.wisp.stream.Sink
import org.wisp.stream.extensions.*
import org.wisp.stream.flow.{FlowProcessor, FlowPublisher, FlowSubscriber}
import org.wisp.stream.graph.StreamGraph
import org.wisp.utils.extensions.*

class FlowTests {

  @Test
  def baseTest(): Unit = {

    ActorSystem() || { sys =>
      val graph = StreamGraph()
      val src = graph.from((1 to 10).asSource)

      val publisher = FlowPublisher(src.link)
      val proc = FlowProcessor(publisher, identity)
      val subscriber = FlowSubscriber(proc)

      var res: List[Int] = Nil
      graph.node(subscriber).toRunnable(Sink{ v =>
        res = v :: res
      }).run()

      Assertions.assertEquals(1 to 10, res.reverse)
    }

  }

}
