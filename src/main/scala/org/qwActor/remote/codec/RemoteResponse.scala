package org.qwActor.remote.codec

import org.qwActor.remote.ObjectId

import java.io.{ObjectInputStream, ObjectOutputStream}

object RemoteResponse extends Deserializer[RemoteResponse]{

  def apply(returnTo:ObjectId, value:Any) = new RemoteResponse(returnTo, value)

  def unapply(m: RemoteResponse): Some[(ObjectId, Any)] = Some((m.returnTo, m.value))

  //override def messageType : MessageType = MessageType.remoteResponse

  override def readFrom(is: ObjectInputStream): RemoteResponse = {
    val returnTo = new ObjectId( is.readLong(), is.readLong(), is.readLong(), is.readLong() )
    val value = is.readObject()

    new RemoteResponse(returnTo, value)
  }

}

@SerialVersionUID(1L)
class RemoteResponse(val returnTo:ObjectId, val value:Any) extends Serializer, Serializable {

  override def messageType : MessageType = MessageType.remoteResponse

  override def writeTo(os: ObjectOutputStream): Unit = {
    os.writeLong(returnTo.seriesId)
    os.writeLong(returnTo.time)
    os.writeLong(returnTo.seq)
    os.writeLong(returnTo.random)

    os.writeObject(value)
  }

  override def toString: String = "RemoteResponse(returnTo="+returnTo+",value="+value+")"
}