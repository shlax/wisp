package org.wisp.remote

import org.wisp.ActorRef
import org.wisp.bus.EventBus

trait RemoteContext extends EventBus{

  def get(path:Any) : ActorRef
}
