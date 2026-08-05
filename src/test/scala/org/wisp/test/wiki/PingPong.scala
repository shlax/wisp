package org.wisp.test.wiki

import org.junit.jupiter.api.Test
import org.wisp.ActorSystem
import org.wisp.utils.extensions.*

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PingPong {

  enum PingPongMessage {
    case Ping, Pong
  }

  @Test
  def pingPong(): Unit = {

    ActorSystem() || { system =>
      val actor = system.apply[PingPongMessage.Ping.type , PingPongMessage.Pong.type]( from => {
        case PingPongMessage.Ping => from << PingPongMessage.Pong
      })
      val result = actor.ask(PingPongMessage.Ping)
      Await.result(result, 1.second)
      println(result.value.get)
    }

  }

}
