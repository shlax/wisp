package org.wisp.io

import org.wisp.closeable.*

import java.io.{ByteArrayInputStream, DataInput, DataInputStream, DataOutput}

object extensions {

  def ioRead[T](in: DataInput)(using rw: ReadWrite[T]): T = {
    rw.read(in)
  }

  def fromBytes[T](buff: Array[Byte])(using rw: ReadWrite[T]): T = {
    new DataInputStream(new ByteArrayInputStream(buff))|{ in => ioRead(in) }
  }

  given [T: ReadWrite] => ReadWrite[Option[T]] = new ReadWrite[Option[T]] {
    override def read(in: DataInput): Option[T] = {
      if(in.readBoolean()){
        val rw = summon[ReadWrite[T]]
        Some(rw.read(in))
      }else None
    }
    override def write(t: Option[T], out: DataOutput): Unit = {
      t match {
        case Some(v) =>
          out.writeBoolean(true)
          val rw = summon[ReadWrite[T]]
          rw.write(v, out)
        case None =>
          out.writeBoolean(false)
      }
    }
  }

  given [T: ReadWrite] => ReadWrite[List[T]] = new ReadWrite[List[T]] {
    override def read(in: DataInput): List[T] = {
      val size = in.readInt()
      if(size > 0){
        val rw = summon[ReadWrite[T]]
        List.fill(size)(rw.read(in))
      }else Nil
    }

    override def write(t: List[T], out: DataOutput): Unit = {
      val size = t.size
      out.writeInt(size)
      if(size > 0){
        val rw = summon[ReadWrite[T]]
        for(i <- t){ rw.write(i, out) }
      }
    }
  }

  given [K: ReadWrite, V:ReadWrite] => ReadWrite[Map[K, V]] = new ReadWrite[Map[K, V]] {
    override def read(in: DataInput): Map[K, V] = {
      val size = in.readInt()
      if(size > 0){
        val rwk = summon[ReadWrite[K]]
        val rwv = summon[ReadWrite[V]]
        val b = Map.newBuilder[K, V]
        b.sizeHint(size)
        var i = 0
        while(i < size){
          b += rwk.read(in) -> rwv.read(in)
          i += 1
        }
        b.result()
      }else Map.empty
    }

    override def write(t: Map[K, V], out: DataOutput): Unit = {
      val size = t.size
      out.writeInt(size)
      if(size > 0){
        val rwk = summon[ReadWrite[K]]
        val rwv = summon[ReadWrite[V]]
        for((k, v) <- t){
          rwk.write(k, out)
          rwv.write(v, out)
        }
      }
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
