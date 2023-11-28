package org.wisp.test.workload.steal

import org.wisp.test.workload
import org.wisp.test.workload.steal.TaskWorker.pull
import org.wisp.{Actor, ActorContext, ActorRef}

import java.util
import java.util.concurrent.{CompletableFuture, Executors, LinkedBlockingQueue}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.function.Supplier
import scala.util.Random

object Task{
  val seq = AtomicInteger()
}

class Task{
  val from = Thread.currentThread().getName
  val n = Task.seq.incrementAndGet()

  def apply(nm:String):List[Task] = {
    println(nm+":>Task("+n+")=>"+from+":"+Thread.currentThread().getName)
    Thread.sleep(Random.nextInt(500))

    if( n < 100 && ( n < 10 || Math.random() < 0.3 ) ){
      var r:List[Task] = Nil
      for(i <- 0 until Random.nextInt(9) + 1) r = new Task :: r
      r
    }else Nil
  }

}

case object PullTask
case class QueueTask(task:Task, State:State)

case class State(nm:String, size:Int)

class TaskBalancer(context: ActorContext) extends Actor(context){

  class WorkerState(val ref:ActorRef) extends Comparable[WorkerState]{
    var size = 0

    override def compareTo(o: WorkerState): Int = this.size.compareTo(o.size)
  }

  val workers: Map[String, WorkerState] = ( for(i <- 1 to 3) yield {
    val ref = context.create(c => new TaskWorker("W"+i, this, c))
    ("W"+i , new WorkerState(ref))
  } ).toMap


  def process(t: Task): Unit = {
    val w = workers.values.min
    w.ref << t
  }

  def state(s: State):Unit = {
    val w = workers(s.nm)
    w.size = s.size

    var x = w.size
    for(z <- workers if z._1 != s.nm if x > 2 && x - z._2.size > 2){
      w.ref << PullTask
      x -= 1
    }

  }

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case t:Task =>
      process(t)

    case QueueTask(t, s) =>
      state(s)
      process(t)

    case s:State =>
      state(s)

  }

}

object TaskWorker{
  val pull = Executors.newFixedThreadPool(3)
}

class TaskWorker(nm:String, balancer:ActorRef, context: ActorContext) extends Actor(context){

  val running = new AtomicBoolean(false)
  val queue = new LinkedBlockingQueue[Task]()

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case t:Task =>
      val r = running.compareAndSet(false, true)
      if(r){
        val wNm = nm+"["+queue.size()+"]"
        CompletableFuture.supplyAsync(() => { t.apply(wNm) }, TaskWorker.pull).thenApply { l =>
          if( ! running.compareAndSet(true, false) ) throw new IllegalStateException("already running")
          for (x <- l) queue.add(x)

          val h = queue.poll()
          if(h != null) TaskWorker.this << h
        }

      }else{
        queue.add(t)
      }

      balancer << State(nm, queue.size())

    case PullTask =>
      val t = queue.poll()
      if(t != null){
        balancer << QueueTask(t, State(nm, queue.size()))
      }
  }

}