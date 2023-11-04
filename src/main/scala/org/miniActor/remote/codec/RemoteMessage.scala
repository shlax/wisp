package org.miniActor.remote.codec

import org.miniActor.remote.ObjectId

import java.io.{ObjectInputStream, ObjectOutputStream}

object RemoteMessage extends Deserializer[RemoteMessage]{

  def apply(path:Any, value:Any, returnTo:Option[ObjectId]) = new RemoteMessage(path, value, returnTo)

  def unapply(m: RemoteMessage): Some[(Any, Any, Option[ObjectId])] = Some((m.path, m.value, m.returnTo))

  //override def messageType : MessageType = MessageType.remoteMessage

  override def readFrom(is: ObjectInputStream): RemoteMessage = {
    val rt = is.readBoolean()
    val returnTo = if(rt) {
      new ObjectId( is.readLong(), is.readLong(), is.readLong(), is.readLong() )
    }else{
      null
    }

    val path = is.readObject()
    val value = is.readObject()

    new RemoteMessage(path, value, Option(returnTo))
  }
}

@SerialVersionUID(1L)
class RemoteMessage(val path:Any, val value:Any, val returnTo:Option[ObjectId]) extends Serializer, Serializable{

  override def messageType : MessageType = MessageType.remoteMessage

  override def writeTo(os: ObjectOutputStream): Unit = {
    if(returnTo.isEmpty){
      os.writeBoolean(false)
    }else {
      os.writeBoolean(true)
      val rt = returnTo.get

      os.writeLong(rt.seriesId)
      os.writeLong(rt.time)
      os.writeLong(rt.seq)
      os.writeLong(rt.random)

    }

    os.writeObject(path)
    os.writeObject(value)
  }

  override def toString: String = "RemoteMessage(path="+path+",value="+value+",returnTo="+returnTo+")"

}
