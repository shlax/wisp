package org.wisp.stream.typed

import org.wisp.{ActorLink, ActorSystem}
import org.wisp.stream.Source
import org.wisp.stream.iterator.{ActorFlow, ActorSink, ActorSource}

import java.util.function.Consumer

class Stream(val system:ActorSystem){

  class Node[T](val link:ActorLink) {

    def map[V](fn: T => V): Node[V] = {
      val r = system.create(i => ActorFlow(link, i, (a:Any) => {
          fn.apply(a.asInstanceOf[T])
        }))
      new Node[V](r)
    }

    def to[V >: T](c:Consumer[V]): ActorSink = {
      ActorSink(link, (a:Any) => {
          c.accept(a.asInstanceOf[V])
        })
    }

    def as[R, V >: Node[T]](fn: V => R) : R = {
      fn.apply(this)
    }

  }

  def from[T](s:Source[T]) : Node[T] = {
    new Node[T](ActorSource(s))
  }

}
