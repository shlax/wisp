package org.wisp.test

import org.scalatest.funsuite.AnyFunSuite
import org.wisp.{Actor, ActorContext, ActorRef, ActorSystem}
import org.wisp.timer.Timer
import scala.concurrent.duration._

class TimerTest extends AnyFunSuite{

  class HelloActor(context: ActorContext) extends Actor(context) {
    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case a =>
        println(">"+a)
    }
  }

  test("timerTest"){
    val t = new Timer()
    val as = new ActorSystem

    val a = as.create(new HelloActor(_))

    t.scheduleMessage(a, "Hello loop", 1.seconds, 1.seconds)
    t.scheduleMessage(a, "Hello once", 2.seconds)

    Thread.sleep(5000)

    t.close()
    Thread.sleep(5000)

    as.close()
  }

}
