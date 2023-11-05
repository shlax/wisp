package org.wisp.test

import org.wisp.{Actor, ActorContext, ActorRef, ActorSystem}
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.CountDownLatch

class RuntimeTest extends AnyFunSuite{

  class HelloActor(context: ActorContext, count:CountDownLatch, name:String) extends Actor(context){
    override def process(sender: ActorRef) : PartialFunction[Any, Unit] = {
      count.countDown()
      {
        case i: Int =>
          sender << "ack"

          for(j <- 0 to i){
            val a = context.create(new HelloActor(_, count, name+"-"+j))
            for(k <- 0 to j) a << name+" hello "+j+" "+k
          }

        case s: String =>
          println(""+Thread.currentThread()+" : "+name+" - "+s)
          sender << List(name+" / "+s)

        case l : List[_] =>
          println(""+Thread.currentThread()+" : "+name+" - "+l)
      }
    }
  }

  test("hello"){
    val cd = new CountDownLatch(23)

    val s = new ActorSystem
    s.create(new HelloActor(_, cd, "A")).ask(3).thenAccept{ m =>
      println(""+Thread.currentThread()+" : thenAccept: "+m.value)
      m.sender << List("ok")
      cd.countDown()
    }

    cd.await()
    s.close()
  }

}
