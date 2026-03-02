package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.stream.iterator.message.End

import java.util

trait NodeFlow {

  protected def nodes:util.Queue[ActorLink]

  protected def sendEnd(exception: Option[Throwable]): Unit = {
    var a = nodes.poll()
    while (a != null) {
      a << End(exception)
      a = nodes.poll()
    }
  }
  
}
