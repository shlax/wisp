package org.wisp.remote

object RemoteMessage {

  def unapply(m: RemoteMessage): (String, Any) = (m.path, m.message)

}

class RemoteMessage(val path:String, val message:Any)
