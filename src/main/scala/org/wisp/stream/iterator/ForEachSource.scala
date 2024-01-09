package org.wisp.stream.iterator

import org.wisp.{ActorMessage, ActorRef}

import java.util
import java.util.concurrent.locks.ReentrantLock

object ForEachSource{

  def apply[T](it:Source[T]):ForEachSource[T] = new ForEachSource(it, new util.LinkedList[ActorRef]())

  def apply[T](it:Source[T], nodes:util.Queue[ActorRef]):ForEachSource[T] = new ForEachSource(it, nodes)

}

class ForEachSource[T](it:Source[T], nodes:util.Queue[ActorRef]) extends ActorRef, Runnable {

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  private var ended = false

  override def run():Unit = {
    it.forEach { e =>
      try {
        lock.lock()
        var a = nodes.poll()
        while (a == null){
          condition.await()
          a = nodes.poll()
        }
        a << Next(e)
      } finally {
        lock.unlock()
      }
    }

    try {
      lock.lock()
      ended = true
      var a = nodes.poll()
      while (a != null) {
        a << End
        a = nodes.poll()
      }
    } finally {
      lock.unlock()
    }
  }

  override def accept(t: ActorMessage): Unit = {
    t.value match {
      case HasNext =>
        try {
          lock.lock()
          if(ended){
            t.sender << End
          }else {
            nodes.add(t.sender)
            condition.signalAll()
          }
        } finally {
          lock.unlock()
        }

    }
  }

}
