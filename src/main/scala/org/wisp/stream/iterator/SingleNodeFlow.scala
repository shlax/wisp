package org.wisp.stream.iterator

import java.util

trait SingleNodeFlow[T] extends StreamFlow[T]{

  protected val nodes:util.Queue[Option[T] => Unit]

  protected def createNodes(): util.Queue[Option[T] => Unit] = {
    util.LinkedList[Option[T] => Unit]()
  }

  protected def sendEnd(): Unit = {
    var a = nodes.poll()
    while (a != null) {
      a.apply(None)
      a = nodes.poll()
    }
  }
  
}
