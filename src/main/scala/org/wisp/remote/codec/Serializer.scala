package org.wisp.remote.codec

import java.io.ObjectOutputStream

trait Serializer {

  def messageType : MessageType

  def writeTo(os:ObjectOutputStream):Unit

}
