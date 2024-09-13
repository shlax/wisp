package org.wisp.remote.bus

import org.wisp.bus.Event
import org.wisp.remote.{AbstractConnection, RemoteSystem}

object RemoteSystemEvent{

  def unapply(m: RemoteSystemEvent): Some[RemoteSystem] = Some(m.remoteSystem)
}

abstract class RemoteSystemEvent(val remoteSystem: RemoteSystem) extends Event{

}
