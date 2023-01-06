package org.qwActor.test.tutorial

import org.qwActor.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Using

class AskHelloWorld extends AnyFunSuite {

  class HelloActor(context: ActorContext) extends Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case a =>
        sender << "hello " + a
    }
  }

  test("askHelloWorld") {
    Using(new ActorSystem) { system =>
      val actorRef = system.create(new HelloActor(_))
      val res = actorRef.ask("world").get()
      println(res.value)
    }.get
  }
}
