package org.wisp.stream.iterator

import java.util

trait SingleNodeFlow[T] {

  protected def nodes:util.Queue[OperationLink[T]]

  protected def createNodes(): util.Queue[OperationLink[T]] = {
    util.LinkedList[OperationLink[T]]()
  }

  protected def sendEnd(): Unit = {
    var a = nodes.poll()
    while (a != null) {
      a << End
      a = nodes.poll()
    }
  }
  
}
