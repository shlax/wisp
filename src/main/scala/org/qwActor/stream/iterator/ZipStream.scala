package org.qwActor.stream.iterator

import org.qwActor.stream.ForEach
import org.qwActor.{Actor, ActorContext, ActorMessage, ActorRef}
import org.qwActor.stream.iterator.messages.{End, HasNext, Next}

import scala.collection.mutable
import java.util
import java.util.concurrent.locks.ReentrantLock

object ZipStream{

  def apply(prev: ForEach[ActorRef], context: ActorContext): ZipStream = {
    new ZipStream(prev, context, new util.LinkedList[ActorRef](), new util.LinkedList[ZipStream#NodeRefValue]())
  }

  def apply(prev: ForEach[ActorRef], context: ActorContext, nodes: util.Queue[ActorRef], values:util.Queue[ZipStream#NodeRefValue]): ZipStream = {
    new ZipStream(prev, context, nodes, values)
  }

}

class ZipStream(prev:ForEach[ActorRef], context: ActorContext, nodes:util.Queue[ActorRef], values:util.Queue[ZipStream#NodeRefValue]) extends Actor(context){

  private val lock = new ReentrantLock()

  class NodeRefValue(n:NodeRef, v: Any) {
    def apply(): Any = {
      n.next()
      v
    }
  }

  class NodeRef(ref:ActorRef) extends ActorRef {
    private var ended:Boolean = false

    def next():Unit = {
      ref.ask(HasNext).thenAccept(this)
    }

    override def accept(t: ActorMessage): Unit = {
      try {
        lock.lock()

        t.value match {
          case Next(v) =>
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

  override def process(sender: ActorRef): PartialFunction[Any, Unit] = {
    case HasNext =>
      try {
        lock.lock()
        if(!started){
          started = true
          for(q <- queues) q.next()
        }

        if(allEnded) {
          sender << End
        }else{
          val v = values.poll()
          if (v == null) {
            nodes.add(sender)
          } else {
            sender << Next(v.apply())
          }
        }
      } finally {
        lock.unlock()
      }

  }

}
