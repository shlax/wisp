package org.wisp.stream

import org.wisp.Consumer

@FunctionalInterface
trait Sink[-T] extends Consumer[T]{

  def flush(): Unit = {}

}
