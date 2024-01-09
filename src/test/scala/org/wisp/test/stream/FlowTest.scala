package org.wisp.test.stream

import org.wisp.{Actor, ActorContext, ActorMessage, ActorRef, ActorSystem, MessageQueue}
import org.wisp.stream.Flow
import org.scalatest.funsuite.AnyFunSuite
import org.wisp.stream.iterator.Source.*

import java.util.concurrent.CountDownLatch

class FlowTest extends AnyFunSuite{

  class PrintActor(cd:CountDownLatch, context: ActorContext) extends Actor(context){
    override def createQueue(): MessageQueue[ActorMessage] = MessageQueue(1)

    override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
      case a =>
        println(">> "+a)
        Thread.sleep(50)
        cd.countDown()
    }
  }

  test("oddEven"){
    val s = new ActorSystem
    val cd = new CountDownLatch(20)

    val a = s.create( c => new PrintActor(cd, c) )

    Flow((1 to 20).asSource){ f =>
      f.filter( _ % 2 == 0).map( "" + _ + " is even").via{ self =>
        self.add( (v, next:Flow[String]) => next("     -> "+v) ).to(println)
        self >> a
      }
      f.filter( _ % 2 == 1).map( "" + _ + " is odd") >> a
    }

    cd.await()
    s.close()
  }

}
