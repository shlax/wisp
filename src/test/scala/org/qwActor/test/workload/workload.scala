package org.qwActor.test.workload

import org.qwActor.stream.WaitBarrier
import org.qwActor.{Actor, ActorContext, ActorRef, MessageQueue}

import java.util.concurrent.CountDownLatch

case class DoWork(work:CountDownLatch, i:Int)
case object RequestWork

class Worker(balancer:ActorRef, nm:String, context: ActorContext) extends Actor(context){
  balancer.ask(RequestWork).thenAccept(this)

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case DoWork(work, i) =>
      println(nm+" "+Thread.currentThread()+" work "+i)
      Thread.sleep(50)
      work.countDown()
      sender << RequestWork // use ask for remote: sender.ask(RequestWork).thenAccept(this)
  }
}

class Balancer(barrier:WaitBarrier[DoWork], context: ActorContext) extends Actor(context){
  barrier.next(this)

  val workers = MessageQueue[ActorRef]()
  val workQueue = MessageQueue[DoWork]()

  def onWorker(sender: ActorRef):Unit = {
    val work = workQueue.poll()
    if(work == null) workers.add(sender)
    else sender << work
  }

  def onWork(work: DoWork): Unit = {
    val worker = workers.poll()
    if (worker == null) workQueue.add(work)
    else worker << work
  }

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case RequestWork =>
      onWorker(sender)
      barrier.next(this)

    case w:DoWork =>
      onWork(w)
  }
}