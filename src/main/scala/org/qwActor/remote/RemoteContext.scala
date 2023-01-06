package org.qwActor.remote

import org.qwActor.ActorRef

trait RemoteContext{

  def get(path:Any) : ActorRef
}
