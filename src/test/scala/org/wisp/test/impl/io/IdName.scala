package org.wisp.test.impl.io

import org.wisp.serializer.ReadWrite
import org.wisp.serializer.given
import org.wisp.remote.RemoteMessage

case class IdName (id:Int, name:String) extends RemoteMessage[Int] derives ReadWrite {
  override def path: Int = id
}
