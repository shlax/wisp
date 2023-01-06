package org.qwActor.remote.codec

import java.io.ObjectInputStream

trait Deserializer[T] {

  //def messageType : MessageType

  def readFrom(is:ObjectInputStream) : T

}
