package org.wisp.stream.typed

import org.wisp.ActorLink
import org.wisp.stream.iterator.{ActorFlow, ActorSink}

import java.util.function.Consumer

class Node[T](val stream: Stream, val link: ActorLink) {

  def map[V, F >: T](fn: F => V): Node[V] = {
    val r = stream.system.create(i => ActorFlow(link, i, (a: Any) => {
      fn.apply(a.asInstanceOf[T])
    }))
    new Node[V](stream, r)
  }

  def to[V >: T](c: Consumer[V]): ActorSink = {
    ActorSink(link, (a: Any) => {
      c.accept(a.asInstanceOf[V])
    })
  }

  def as[R, V >: Node[? >: T]](fn: V => R): R = {
    fn.apply(this)
  }

}