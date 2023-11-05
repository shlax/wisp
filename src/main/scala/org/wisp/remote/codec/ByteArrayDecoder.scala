package org.wisp.remote.codec

import org.wisp.remote.ObjectId

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.nio.ByteBuffer
import java.util.function.Consumer
import scala.util.Using

class ByteArrayDecoder(consumer: Consumer[Any]) extends Decoder {

  private val int = ByteBuffer.allocate(4)

  private var tpe: Option[MessageType] = None
  private var body: Option[ByteBuffer] =  None

  protected def read[T](b:ByteBuffer, d:Deserializer[T]): T = {
    val baIs = new ByteArrayInputStream(b.array())
    Using(new ObjectInputStream(baIs)) { is =>
      d.readFrom(is)
    }.get
  }

  protected def decode(t: MessageType, b:ByteBuffer):Any = t match {
    case MessageType.objectId =>
      ObjectId(b.getLong, b.getLong, b.getLong, b.getLong)
    case MessageType.remoteMessage =>
      read(b, RemoteMessage)
    case MessageType.remoteResponse =>
      read(b, RemoteResponse)
  }

  override def read(buffer: ByteBuffer): Boolean = {
    tpe match {
      case Some(t) =>
        body match {
          case Some(b) =>
            copy(buffer, b)
            if (b.remaining() == 0) {
              b.flip()
              consumer.accept(decode(t, b))

              tpe = None
              body = None
            }

          case None =>
            copy(buffer, int)
            if (int.remaining() == 0) {
              int.flip()
              body = Some(ByteBuffer.allocate(int.getInt))
              int.clear()
            }

        }

        if(buffer.hasRemaining) read(buffer)
        else true

      case None =>
        var next = true

        copy(buffer, int)

        if(int.remaining() == 0){
          int.flip()
          val code = int.getInt

          if(code == -1) next = false
          else {
            val mt = MessageType.fromOrdinal(code)
            tpe = Some(mt)

            val len = mt.size
            if (len > 0) body = Some(ByteBuffer.allocate(len))
          }

          int.clear()
        }

        if(buffer.hasRemaining) read(buffer)
        else next
    }
  }

  protected def copy(from: ByteBuffer, to:ByteBuffer):Unit = {
    val pos = to.position()
    val off = from.position()

    val len = Math.min(from.remaining(), to.remaining())

    to.put(pos, from, off, len)

    to.position(pos + len)
    from.position(off + len)
  }

}
