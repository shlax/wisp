package org.wisp.stream.iterator

import org.wisp.Link

import java.util

trait SingleNodeFlow[T] {

  protected def nodes:util.Queue[Link[Operation[T], Operation[T]]]

  protected def createNodes(): util.Queue[Link[Operation[T], Operation[T]]] = {
    util.LinkedList[Link[Operation[T], Operation[T]]]()
  }

  protected def sendEnd(): Unit = {
    var a = nodes.poll()
    while (a != null) {
      a << End
      a = nodes.poll()
    }
  }
  
}
