package org.wisp.remote

import org.wisp.ActorRef

trait RemoteContext{

  def get(path:Any) : ActorRef
}
