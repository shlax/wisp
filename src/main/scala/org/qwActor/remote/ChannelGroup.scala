package org.qwActor.remote

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

object ChannelGroup {

  def newSingleThreadChannelGroup(): AsynchronousChannelGroup = {
      AsynchronousChannelGroup.withFixedThreadPool(1, Executors.defaultThreadFactory())
  }

}

trait ChannelGroup extends AutoCloseable{

  protected def getChannelGroup(): AsynchronousChannelGroup = ChannelGroup.newSingleThreadChannelGroup()
  protected def channelGroup : AsynchronousChannelGroup
  
  protected def shutdownChannelGroup(): Boolean = true

  override def close(): Unit = {
    if(shutdownChannelGroup()) channelGroup.shutdown()
  }
}
