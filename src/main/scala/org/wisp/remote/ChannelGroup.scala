package org.wisp.remote

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

object ChannelGroup {

  def newSingleThreadChannelGroup(): AsynchronousChannelGroup = {
      AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory())
  }

}

trait ChannelGroup {

  protected def createChannelGroup(): AsynchronousChannelGroup = ChannelGroup.newSingleThreadChannelGroup()
  protected def channelGroup : AsynchronousChannelGroup
  
  protected def shutdownChannelGroup(): Unit = {
    channelGroup.shutdown()
  }

}
