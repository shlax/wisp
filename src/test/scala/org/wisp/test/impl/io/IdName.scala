package org.wisp.test.impl.io

import org.wisp.io.ReadWrite
import org.wisp.io.extensions.given
import org.wisp.remote.RemoteMessage

case class IdName (id:Int, name:String) extends RemoteMessage[Int] derives ReadWrite {
  override def path: Int = id
}
