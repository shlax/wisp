package org.wisp.remote.codec

import org.wisp.remote.ObjectId

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import scala.util.Using

class ByteArrayEncoder(value:Any) extends Encoder{
  private val disconnect = value == Disconnect

  private val data: Array[Byte] = value match {
    case Disconnect =>
      val b = ByteBuffer.allocate(4)
      b.putInt(-1)
      b.array()

    case ObjectId(series, time, seq, rand) =>
      val b = ByteBuffer.allocate(4 + MessageType.objectId.size)
      b.putInt(MessageType.objectId.ordinal)
      b.putLong(series)
      b.putLong(time)
      b.putLong(seq)
      b.putLong(rand)
      b.array()

    case s : Serializer =>
      val bos = new ByteArrayOutputStream()
      Using(new ObjectOutputStream(bos)){ os =>
        s.writeTo(os)
      }.get
      val arr = bos.toByteArray

      val b = ByteBuffer.allocate(4 + 4 + arr.length)
      b.putInt(s.messageType.ordinal)
      b.putInt(arr.length)
      b.put(arr)
      b.array()
  }

  private var off: Int = 0

  override def write(buffer: ByteBuffer): Option[Boolean] = {
    val size = Math.min(buffer.remaining(), data.length - off)
    buffer.put(data, off, size)

    off += size

    if(off == data.length){
      if (!disconnect) Some(true) else None
    }else Some(false)

  }

}
