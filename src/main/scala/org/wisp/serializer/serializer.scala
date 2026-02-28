package org.wisp

import org.wisp.closeable.*

import java.io.{ByteArrayInputStream, DataInput, DataInputStream, DataOutput}
import java.util.zip.{Inflater, InflaterInputStream}

/**
 * Package object providing serialization utilities and given instances for common types.
 */
package object serializer {

  /**
   * Reads a value of type T from a DataInput stream using the implicit ReadWrite instance.
   *
   * @tparam T the type of value to read
   * @param dataInput the DataInput stream to read from
   * @param readWrite the implicit ReadWrite instance for type T
   * @return the deserialized value of type T
   */
  def readFrom[T](dataInput: DataInput)(using readWrite: ReadWrite[T]): T = {
    readWrite.read(dataInput)
  }


  /**
   * Deserializes a value of type T from a byte array with optional compression support.
   *
   * The byte array format is:
   * - First byte: compression flag (1 if compressed, 0 if uncompressed)
   * - Remaining bytes: the serialized data (either compressed or uncompressed)
   *
   * @tparam T the type of value to deserialize
   * @param data   the byte array containing the serialized data
   * @param offest the starting offset in the byte array (default: 0)
   * @param length the number of bytes to read (default: data.length)
   * @param readWrite     the implicit ReadWrite instance for type T
   * @return the deserialized value of type T
   */
  def fromBytes[T](data: Array[Byte], offest:Int = -1, length: Int = -1)(using readWrite: ReadWrite[T]): T = {
    val off = if(offest >= 0) offest else 0
    val len = if(length >= 0) length else data.length
    val in = new ByteArrayInputStream(data, 1 + off, len - 1)
    if(data(0 + off) == 0){
      new DataInputStream(in) | { in =>
        readFrom(in)
      }
    }else{
      new InflaterInputStream(in, new Inflater(true))|{ zip =>
        new DataInputStream(zip) | { in =>
          readFrom(in)
        }
      }
    }
  }

  /**
   * Given instance for serializing and deserializing Option[T] values.
   *
   * @tparam T the type of the optional value
   */
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

  /**
   * Given instance for serializing and deserializing List[T] values.
   *
   * @tparam T the type of list elements
   */
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

  /**
   * Given instance for serializing and deserializing Map[K, V] values.
   *
   * @tparam K the type of map keys
   * @tparam V the type of map values
   */
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

  /**
   * Given instance for serializing and deserializing Int
   */
  given ReadWrite[Int] = new ReadWrite[Int] {
    override def read(in: DataInput): Int = in.readInt()
    override def write(t: Int, out: DataOutput): Unit = out.writeInt(t)
  }

  /**
   * Given instance for serializing and deserializing Boolean
   */
  given ReadWrite[Boolean] = new ReadWrite[Boolean] {
    override def read(in: DataInput): Boolean = in.readBoolean()
    override def write(t: Boolean, out: DataOutput): Unit = out.writeBoolean(t)
  }

  /**
   * Given instance for serializing and deserializing String
   */
  given ReadWrite[String] = new ReadWrite[String] {
    override def read(in: DataInput): String = in.readUTF()
    override def write(t: String, out: DataOutput): Unit = out.writeUTF(t)
  }

  /**
   * Given instance for serializing and deserializing Long
   */
  given ReadWrite[Long] = new ReadWrite[Long] {
    override def read(in: DataInput): Long = in.readLong()
    override def write(t: Long, out: DataOutput): Unit = out.writeLong(t)
  }

  /**
   * Given instance for serializing and deserializing Short
   */
  given ReadWrite[Short] = new ReadWrite[Short] {
    override def read(in: DataInput): Short = in.readShort()
    override def write(t: Short, out: DataOutput): Unit = out.writeShort(t)
  }

  /**
   * Given instance for serializing and deserializing Char
   */
  given ReadWrite[Char] = new ReadWrite[Char] {
    override def read(in: DataInput): Char = in.readChar()
    override def write(t: Char, out: DataOutput): Unit = out.writeChar(t)
  }

  /**
   * Given instance for serializing and deserializing Byte
   */
  given ReadWrite[Byte] = new ReadWrite[Byte] {
    override def read(in: DataInput): Byte = in.readByte()
    override def write(t: Byte, out: DataOutput): Unit = out.writeByte(t)
  }

  /**
   * Given instance for serializing and deserializing Float
   */
  given ReadWrite[Float] = new ReadWrite[Float] {
    override def read(in: DataInput): Float = in.readFloat()
    override def write(t: Float, out: DataOutput): Unit = out.writeFloat(t)
  }

  /**
   * Given instance for serializing and deserializing Double
   */
  given ReadWrite[Double] = new ReadWrite[Double] {
    override def read(in: DataInput): Double = in.readDouble()
    override def write(t: Double, out: DataOutput): Unit = out.writeDouble(t)
  }

}
