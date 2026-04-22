package org.wisp.stream.iterator

import org.wisp.ActorLink

import java.util

trait SingleNodeFlow[T] {

  protected def nodes:util.Queue[ActorLink[Operation[T]]]

  protected def createNodes(): util.Queue[ActorLink[Operation[T]]] = {
    util.LinkedList[ActorLink[Operation[T]]]()
  }

  protected def sendEnd(): Unit = {
    var a = nodes.poll()
    while (a != null) {
      a << End
      a = nodes.poll()
    }
  }
  
}
