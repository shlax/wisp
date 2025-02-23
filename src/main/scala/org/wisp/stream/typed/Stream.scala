package org.wisp.stream.typed

import org.wisp.ActorSystem
import org.wisp.stream.Source
import org.wisp.stream.iterator.{ActorSource, ForEachSource}

import java.util.function.Consumer

class Stream(val system:ActorSystem){

  def from[T](s:Source[? <: T]) : Node[T] = {
    Node[T](this, ActorSource(s))
  }

  def forEach[T, V >: Node[ ? >: T]](s:Source[? <: T])(fn : Consumer[V]) : ForEachSource = {
    val f = ForEachSource(s)
    fn.accept( Node[T](this, f) )
    f
  }

}
