package org.wisp.stream.iterator

import org.wisp.bus.EventBus
import org.wisp.{ActorMessage, ActorRef}

import scala.collection.mutable
import java.util
import java.util.concurrent.locks.ReentrantLock

object ZipStream{

  def apply(bus:EventBus, prev: Source[ActorRef]): ZipStream = {
    new ZipStream(bus, prev, new util.LinkedList[ActorRef](), new util.LinkedList[ZipStream#NodeRefValue]())
  }

  def apply(bus:EventBus, prev: Source[ActorRef], nodes: util.Queue[ActorRef], values:util.Queue[ZipStream#NodeRefValue]): ZipStream = {
    new ZipStream(bus, prev, nodes, values)
  }

}

class ZipStream(bus:EventBus, prev:Source[ActorRef], nodes:util.Queue[ActorRef], values:util.Queue[ZipStream#NodeRefValue]) extends ActorRef(bus){

  private val lock = new ReentrantLock()

  class NodeRefValue(n:NodeRef, v: Any) {
    def apply(): Any = {
      n.next()
      v
    }
  }

  class NodeRef(ref:ActorRef) extends ActorRef(ref.eventBus) {
    private var ended:Boolean = false

    def next():Unit = {
      ref.ask(HasNext).thenAccept(this)
    }

    override def accept(t: ActorMessage): Unit = {
      lock.lock()
      try {
        t.value match {
          case Next(v) =>
            if(ended) throw new IllegalStateException("ended")

            val n = nodes.poll()
            if (n == null) {
              values.add(new NodeRefValue(this, v))
            } else {
              next()
              n << Next(v)
            }

          case End =>
            ended = true

            if(queues.forall(_.ended)){
              allEnded = true

              var n = nodes.poll()
              while(n != null) {
                n << End
                n = nodes.poll()
              }
            }
        }

      } finally {
        lock.unlock()
      }
    }

  }

  private val queues:Array[NodeRef] = {
    val l = new mutable.ArrayBuffer[NodeRef]
    prev.forEach { n => l += new NodeRef(n) }
    l.toArray
  }

  private var started = false
  private var allEnded = false


  override def accept(t: ActorMessage): Unit = {
    lock.lock()
    try {
      t.value match {
        case HasNext =>
          if (!started) {
            started = true
            for (q <- queues) q.next()
          }

          if (allEnded) {
            t.sender << End
          } else {
            val v = values.poll()
            if (v == null) {
              nodes.add(t.sender)
            } else {
              t.sender << Next(v.apply())
            }
          }
      }
    }finally{
      lock.unlock()
    }
  }

}
