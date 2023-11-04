package org.miniActor.remote

import org.miniActor.ActorRef

trait RemoteContext{

  def get(path:Any) : ActorRef
}
