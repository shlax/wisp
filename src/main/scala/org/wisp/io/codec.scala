package org.wisp.io

import org.wisp.closeable.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

object codec {

  def ioRead[T](in: ObjectInputStream)(using rw: ReadWrite[T]): T = {
    rw.read(in)
  }

  def fromBytes[T](in: Array[Byte])(using rw: ReadWrite[T]): T = {
    new ObjectInputStream(new ByteArrayInputStream(in))|{ oin =>
      ioRead(oin)
    }
  }

  extension [T](t: T)(using rw: ReadWrite[T]){
    def ioWrite(out: ObjectOutputStream): Unit = {
      rw.write(t, out)
    }

    def toBytes: Array[Byte] = {
      val bOut = new ByteArrayOutputStream()
      new ObjectOutputStream(bOut)|{ out =>
        ioWrite(out)
      }
      bOut.toByteArray
    }

  }

  given ReadWrite[Int] = new ReadWrite[Int] {
    override def read(in: ObjectInputStream): Int = {
      in.readInt()
    }
    override def write(t: Int, out: ObjectOutputStream): Unit = {
      out.writeInt(t)
    }
  }

  given ReadWrite[Boolean] = new ReadWrite[Boolean] {
    override def read(in: ObjectInputStream): Boolean = {
      in.readBoolean()
    }

    override def write(t: Boolean, out: ObjectOutputStream): Unit = {
      out.writeBoolean(t)
    }
  }

  given ReadWrite[String] = new ReadWrite[String] {
    override def read(in: ObjectInputStream): String = {
      in.readUTF()
    }

    override def write(t: String, out: ObjectOutputStream): Unit = {
      out.writeUTF(t)
    }
  }

  given ReadWrite[Long] = new ReadWrite[Long] {
    override def read(in: ObjectInputStream): Long = {
      in.readLong()
    }

    override def write(t: Long, out: ObjectOutputStream): Unit = {
      out.writeLong(t)
    }
  }

  given ReadWrite[Short] = new ReadWrite[Short] {
    override def read(in: ObjectInputStream): Short = {
      in.readShort()
    }

    override def write(t: Short, out: ObjectOutputStream): Unit = {
      out.writeShort(t)
    }
  }

  given ReadWrite[Char] = new ReadWrite[Char] {
    override def read(in: ObjectInputStream): Char = {
      in.readChar()
    }

    override def write(t: Char, out: ObjectOutputStream): Unit = {
      out.writeChar(t)
    }
  }

  given ReadWrite[Byte] = new ReadWrite[Byte] {
    override def read(in: ObjectInputStream): Byte = {
      in.readByte()
    }

    override def write(t: Byte, out: ObjectOutputStream): Unit = {
      out.writeByte(t)
    }
  }

  given ReadWrite[Float] = new ReadWrite[Float] {
    override def read(in: ObjectInputStream): Float = {
      in.readFloat()
    }

    override def write(t: Float, out: ObjectOutputStream): Unit = {
      out.writeFloat(t)
    }
  }

  given ReadWrite[Double] = new ReadWrite[Double] {
    override def read(in: ObjectInputStream): Double = {
      in.readDouble()
    }

    override def write(t: Double, out: ObjectOutputStream): Unit = {
      out.writeDouble(t)
    }
  }

}
