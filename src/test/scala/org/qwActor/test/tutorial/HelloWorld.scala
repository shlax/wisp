package org.qwActor.test.tutorial

import org.qwActor.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Using

class HelloWorld extends AnyFunSuite{

  class HelloActor(context: ActorContext) extends Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case a => println("hello " + a)
    }
  }

  test("helloWorld"){
    Using(new ActorSystem) { system =>
      val actorRef = system.create(new HelloActor(_))
      actorRef << "world"
    }.get
  }

}
