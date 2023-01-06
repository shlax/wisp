package org.qwActor.test.stream.actor

import org.qwActor.ActorSystem
import org.scalatest.funsuite.AnyFunSuite
import org.qwActor.stream.Flow

import scala.util.Using

class ActorFlowTest extends AnyFunSuite{

  test("actorFlow"){
    Using(ActorSystem()){ s =>

      val strSource = new StringSource((1 to 10).map(_.toString).iterator)
      val strToInt = s.create(c => new StringToInt(strSource, c))
      val intSink = new IntSink(strToInt)(println)

      intSink.start().get()

    }.get
  }

}
