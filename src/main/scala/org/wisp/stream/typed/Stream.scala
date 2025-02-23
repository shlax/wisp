package org.wisp.stream.typed

import org.wisp.ActorSystem
import org.wisp.stream.Source
import org.wisp.stream.iterator.{ActorSource, ForEachSink, ForEachSource}

import java.util.function.Consumer

class Stream(val system:ActorSystem){

  def from[T](s:Source[T]) : Node[T] = {
    Node[T](this, ActorSource(s))
  }

  def forEach[T](s:Source[T])(fn : Node[T] => Unit ) : ForEachSource = {
    val f = ForEachSource(s)
    fn.apply( Node[T](this, f) )
    f
  }

  def forEach[T, R](s:Source[T], c:Consumer[R])(fn: Node[T] => Node[R]) : ForEachSink = {
    ForEachSink(s, (a: Any) => c.accept(a.asInstanceOf[R]) ){ prev =>
        fn.apply(Node[T](this, prev)).link
      }
  }

}
