package org.qwActor.test.stream.actor

import org.qwActor.stream.Barrier
import org.qwActor.{Actor, ActorContext, ActorMessage, ActorRef, MessageQueue}

import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

case object HasNext
case class Next(v:Any)
case object End

class StringSource(it:Iterator[String]) extends ActorRef {

  override def accept(t: ActorMessage): Unit = {
    t.value match {
      case HasNext =>
        if (it.hasNext) {
          val n = it.next()
          t.sender << Next(n)
        } else {
          t.sender << End
        }
    }
  }
}

class StringToInt(prev:ActorRef, context: ActorContext) extends Actor(context){
  var worker:Option[ActorRef] = None

  def onWorker(sender: ActorRef): Unit = {
    if(worker.isDefined) throw new RuntimeException("worker.isDefined")
    worker = Some(sender)
    prev.accept(ActorMessage(this, HasNext))
  }

  def onWork(w: Int): Unit = {
    worker.get << Next(w)
    worker = None
  }

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case Next(v) =>
      v match {
        case s : String =>
          val i = s.toInt
          onWork(i)
      }

    case HasNext =>
      onWorker(sender)

    case End =>
      worker.get << End
  }
}

class IntSink(prev:ActorRef)(fn: Int => Unit) extends ActorRef {
  val cf = new CompletableFuture[Void]

  def start():CompletableFuture[Void] = {
    next()
    cf
  }

  def next():Unit = {
    prev.accept(ActorMessage(this, HasNext))
  }

  override def accept(t: ActorMessage): Unit = {
    t.value match {
      case Next(v) =>
        v match {
          case i: Int =>
            fn.apply(i)
            next()
        }
      case End =>
        cf.complete(null)
    }
  }
}
