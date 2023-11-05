package org.wisp.stream

import java.util
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

object WaitBarrier{

  def apply[T]() : WaitBarrier[T] = {
    apply(new util.LinkedList[Consumer[T]]())
  }

  def apply[T](nodes:util.Queue[Consumer[T]]) : WaitBarrier[T] = {
    new WaitBarrier[T](nodes)
  }

}

/** backpressure mechanism by blocking thread calling {{{ def accept(t: T): Unit }}} */
class WaitBarrier[T](nodes:util.Queue[Consumer[T]]) extends Barrier[T] {

  private val lock = new ReentrantLock()
  private val condition = lock.newCondition()

  override def nextTo(r:Consumer[T]):Unit = {
    try{
      lock.lock()
      nodes.add(r)
      condition.signalAll()
    }finally {
      lock.unlock()
    }
  }

  override def accept(t: T): Unit = {
    var n: Consumer[T] = null
    try{
      lock.lock()
      n = nodes.poll()
      while(n == null){
        condition.await()
        n = nodes.poll()
      }
    } finally {
      lock.unlock()
    }
    n.accept(t)
  }

}
