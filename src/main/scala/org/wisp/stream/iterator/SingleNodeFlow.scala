package org.wisp.stream.iterator

import java.util

trait SingleNodeFlow[T] extends StreamFlow[T]{

  protected def nodes:util.Queue[Response[T] => Unit]

  protected def createNodes(): util.Queue[Response[T] => Unit] = {
    util.LinkedList[Response[T] => Unit]()
  }

  protected def sendEnd(): Unit = {
    var a = nodes.poll()
    while (a != null) {
      a.apply(End)
      a = nodes.poll()
    }
  }
  
}
