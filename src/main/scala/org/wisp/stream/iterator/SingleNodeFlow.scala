package org.wisp.stream.iterator

import org.wisp.ActorLink
import org.wisp.stream.iterator.message.End

import java.util

trait SingleNodeFlow {

  protected def nodes:util.Queue[ActorLink]

  protected def createNodes(): util.Queue[ActorLink] = {
    util.LinkedList[ActorLink]()
  }

  protected def sendEnd(): Unit = {
    var a = nodes.poll()
    while (a != null) {
      a << End
      a = nodes.poll()
    }
  }
  
}
