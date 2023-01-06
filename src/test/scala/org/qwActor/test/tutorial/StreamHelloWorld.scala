package org.qwActor.test.tutorial

import org.qwActor.stream.{WaitBarrier, Flow}
import org.qwActor.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Using

class StreamHelloWorld extends AnyFunSuite {

  case object GetResult

  class SumActor(backpressure:WaitBarrier[Int], context: ActorContext) extends Actor(context) {
    backpressure.next(this)

    var sum = 0

    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case i: Int =>
        backpressure.next(this)
        sum += i
      case GetResult =>
        sender << sum
    }
  }

  test("streamHelloWorld") {
      Using(new ActorSystem) { system =>
        
        val backpressure = WaitBarrier[Int]()
        val sumActor = system.create(c => new SumActor(backpressure, c))

        Flow(1 to 11) { f =>
          f.filter(n => n % 2 == 1).to(backpressure)
        }

        // messages from same thread will be processed in order
        val x = sumActor.ask(GetResult).get().value
        println(x)

      }.get
  }

}
