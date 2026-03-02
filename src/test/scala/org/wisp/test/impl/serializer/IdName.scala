package org.wisp.test.impl.serializer

import org.wisp.serializer.{ReadWrite, given}
import org.wisp.remote.RemoteMessage
import org.wisp.utils.FromMap

case class IdName(id:Int, name:String) extends RemoteMessage[Int] derives ReadWrite, FromMap {
  override def path: Int = id
}
