package org.wisp.stream

import org.wisp.Consumer
import java.io.Flushable

@FunctionalInterface
trait Sink[-T] extends Consumer[T], Flushable{

  override def flush(): Unit = {}

}
