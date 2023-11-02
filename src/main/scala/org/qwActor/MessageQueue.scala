package org.qwActor

import java.util

object MessageQueue{

  class SimpleQueue[E](ll:util.Queue[E]) extends MessageQueue[E]{
    override def size(): Int = ll.size()
    override def poll(): E = ll.poll()
    override def add(e: E): Unit = ll.add(e)
    override def put(e: E): Boolean = ll.add(e)
  }

  def apply[E](): MessageQueue[E] = apply(new util.LinkedList[E]())
  def apply[E](ll:util.Queue[E]): MessageQueue[E] = new SimpleQueue[E](ll)

  def apply[E](maxSize:Int): MessageQueue[E] = apply(maxSize, new util.LinkedList[E]())
  def apply[E](maxSize:Int, ll:util.Queue[E]): MessageQueue[E] = new SimpleQueue[E](ll){
    override def put(e:E):Boolean = {
      if (ll.size() < maxSize) super.put(e)
      else false
    }
  }

}

trait MessageQueue[E] {

  def poll():E

  def add(e:E):Unit

  def put(e:E): Boolean

  def size():Int

}
