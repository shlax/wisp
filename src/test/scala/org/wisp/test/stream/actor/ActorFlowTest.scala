package org.wisp.test.stream.actor

import org.wisp.ActorSystem
import org.scalatest.funsuite.AnyFunSuite
import org.wisp.stream.Flow

import scala.util.Using

class ActorFlowTest extends AnyFunSuite{

  test("actorFlow"){
    Using(ActorSystem()){ s =>

      val strSource = new StringSource(s, (1 to 10).map(_.toString).iterator)
      val strToInt = s.create(c => new StringToInt(strSource, c))
      val intSink = new IntSink(strToInt)(println)

      intSink.start().get()

    }.get
  }

}
