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

case class UpdateState(nm:String, size:Int) // ask
case class QueueTask(t: Task,nm: String, size: Int)  // ask

case class EnqueueTask(t: Task) // response
case object DequeTask // response
case object StateUpdated // no-op response to remove ask from network binding map : org.wisp.remote.client.AskActorRef.accept

class TaskBalancer(context: ActorContext) extends Actor(context) {

  case class WorkerRef(ref:ActorRef, size:Int)

  val workers = mutable.Map[String, WorkerRef]()

  def rebalance():Unit = {
    if(workers.size > 1){
      val max = workers.maxBy(_._2.size)
      if(max._2.size > 2){
        val min = workers.minBy(_._2.size)
        if(max._2.size - min._2.size > 2){
          val x = workers.remove(max._1)
          x.get.ref << DequeTask
        }
      }
    }
  }

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {

    case UpdateState(nm, size) =>
      val x = workers.put(nm, WorkerRef(sender, size) )
      for(w <- x) w.ref << StateUpdated // discard old state

      rebalance()

    case QueueTask(t, nm, size) =>
      val x = workers.put(nm, WorkerRef(sender, size))
      for (w <- x) w.ref << StateUpdated // discard old state

      val m = workers.minBy(_._2.size)
      val w = workers.remove(m._1)
      w.get.ref << EnqueueTask(t)

      rebalance()

  }

}

object TaskWorker {
  val pull = Executors.newFixedThreadPool(3)
}

case object Notify

class TaskWorker(nm:String, balancer:ActorRef, context: ActorContext) extends Actor(context){
  val running = new AtomicBoolean(false)
  val queue = new java.util.LinkedList[Task]()

  def updateState(): Unit = {
    balancer.ask(UpdateState(nm, queue.size())).thenAccept(this)
  }

  updateState()

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
        updateState()
      }

    case Notify =>
      val t = queue.poll()
      if(t != null && runTask(t)){
        updateState()
      }

    case DequeTask =>
      val t = queue.poll()
      if(t != null){
        balancer.ask(QueueTask(t, nm, queue.size())).thenAccept(this)
      }else{
        updateState()
      }

    case EnqueueTask(t) =>
      runTask(t)
      updateState()

    case StateUpdated =>
      // no-op

  }

}
