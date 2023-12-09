package org.wisp.test.workload.steal2

import org.wisp.{Actor, ActorContext, ActorRef}
import java.util.concurrent.{CompletableFuture, Executors}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.collection.mutable
import scala.util.Random

object Task{
  val seq = AtomicInteger()
}

class Task(e: TaskExecutor){
  val from = Thread.currentThread().getName
  val n = Task.seq.incrementAndGet()

  def apply(nm:String) = {
    println(nm+":>Task("+n+")=>"+from+":"+Thread.currentThread().getName)
    Thread.sleep(Random.nextInt(500))

    if( n < 100 && ( n < 10 || Math.random() < 0.3 ) ){
      for(i <- 0 until Random.nextInt(9) + 1){
        e.submit(new Task(e))
      }
    }
  }

}

class TaskExecutor(worker:ActorRef){

  def submit(t:Task):Unit = {
    worker << t
  }

}

case class UpdateState(nm:String, size:Int)
case class QueueTask(t:Task, nm:String, size:Int)
case object PullTask

class TaskBalancer(context: ActorContext) extends Actor(context) {

  class WorkerRef(val ref:ActorRef, var size:Int)

  val workers = mutable.Map[String, WorkerRef]()

  def rebalance():Unit = {
    val max = workers.values.maxBy(_.size)
    if(max.size > 1){
      val min = workers.values.minBy(_.size)
      if(max.size - min.size >= 1){
        max.ref << PullTask
      }
    }
  }

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {

    case UpdateState(nm, size) =>
      workers.update(nm, new WorkerRef(sender, size) )
      rebalance()

    case QueueTask(t, nm, size) =>
      workers.update(nm, new WorkerRef(sender, size))
      workers.values.minBy(_.size).ref << t
      rebalance()

  }

}

object TaskWorker {
  val pull = Executors.newFixedThreadPool(3)
}

case object Notify

class TaskWorker(nm:String, balancerRef:ActorRef, context: ActorContext) extends Actor(context){
  val running = new AtomicBoolean(false)
  val queue = new java.util.LinkedList[Task]()

  val balancer = wrap(balancerRef)
  balancer << UpdateState(nm, queue.size()) // register

  def runTask(t:Task): Boolean = {
    if ( running.compareAndSet(false, true) ) {
      val wNm = nm + "[" + queue.size() + "]"
      CompletableFuture.supplyAsync(() => {
        t.apply(wNm)
      }, TaskWorker.pull).thenApply { l =>
        if (!running.compareAndSet(true, false)) throw new IllegalStateException("already running")
        TaskWorker.this << Notify
      }
      true
    } else {
      queue.add(t)
      false
    }
  }

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case t:Task =>
      if(!runTask(t)){
        balancer << UpdateState(nm, queue.size())
      }

    case Notify =>
      val t = queue.poll()
      if(t != null && runTask(t)){
        balancer << UpdateState(nm, queue.size())
      }

    case PullTask =>
      val t = queue.poll()
      if(t != null){
        balancer << QueueTask(t, nm, queue.size())
      }else{
        balancer << UpdateState(nm, queue.size())
      }

  }

}
