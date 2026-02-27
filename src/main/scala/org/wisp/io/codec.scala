package org.wisp.io

import org.wisp.closeable.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInput, DataInputStream, DataOutput, DataOutputStream}

object codec {

  def ioRead[T](in: DataInput)(using rw: ReadWrite[T]): T = {
    rw.read(in)
  }

  def fromBytes[T](buff: Array[Byte])(using rw: ReadWrite[T]): T = {
    new DataInputStream(new ByteArrayInputStream(buff))|{ in => ioRead(in) }
  }

  extension [T](t: T)(using rw: ReadWrite[T]){

    def ioWrite(out: DataOutput): Unit = {
      rw.write(t, out)
    }

    def toBytes: Array[Byte] = {
      val bOut = new ByteArrayOutputStream()
      new DataOutputStream(bOut)|{ out => ioWrite(out) }
      bOut.toByteArray
    }

  }

  given ReadWrite[Int] = new ReadWrite[Int] {
    override def read(in: DataInput): Int = in.readInt()
    override def write(t: Int, out: DataOutput): Unit = out.writeInt(t)
  }

  given ReadWrite[Boolean] = new ReadWrite[Boolean] {
    override def read(in: DataInput): Boolean = in.readBoolean()
    override def write(t: Boolean, out: DataOutput): Unit = out.writeBoolean(t)
  }

  given ReadWrite[String] = new ReadWrite[String] {
    override def read(in: DataInput): String = in.readUTF()
    override def write(t: String, out: DataOutput): Unit = out.writeUTF(t)
  }

  given ReadWrite[Long] = new ReadWrite[Long] {
    override def read(in: DataInput): Long = in.readLong()
    override def write(t: Long, out: DataOutput): Unit = out.writeLong(t)
  }

  given ReadWrite[Short] = new ReadWrite[Short] {
    override def read(in: DataInput): Short = in.readShort()
    override def write(t: Short, out: DataOutput): Unit = out.writeShort(t)
  }

  given ReadWrite[Char] = new ReadWrite[Char] {
    override def read(in: DataInput): Char = in.readChar()
    override def write(t: Char, out: DataOutput): Unit = out.writeChar(t)
  }

  given ReadWrite[Byte] = new ReadWrite[Byte] {
    override def read(in: DataInput): Byte = in.readByte()
    override def write(t: Byte, out: DataOutput): Unit = out.writeByte(t)
  }

  given ReadWrite[Float] = new ReadWrite[Float] {
    override def read(in: DataInput): Float = in.readFloat()
    override def write(t: Float, out: DataOutput): Unit = out.writeFloat(t)
  }

  given ReadWrite[Double] = new ReadWrite[Double] {
    override def read(in: DataInput): Double = in.readDouble()
    override def write(t: Double, out: DataOutput): Unit = out.writeDouble(t)
  }

}
