package org.wisp.stream

import java.io.Flushable
import java.util.function.Consumer

@FunctionalInterface
trait Sink[T] extends Consumer[T], Flushable, AutoCloseable{

  override def flush(): Unit = {}

  override def close(): Unit = {}
}
