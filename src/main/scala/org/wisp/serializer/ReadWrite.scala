package org.wisp.serializer

import org.wisp.closeable.*

import java.io.{ByteArrayOutputStream, DataInput, DataOutput, DataOutputStream}
import java.util.zip.{Deflater, DeflaterOutputStream}
import scala.compiletime.*
import scala.deriving.*


/**
 * A type class for reading and writing values of type T to/from binary streams.
 */
trait ReadWrite[T] {

  /**
   * Reads a value of type T from the given DataInput stream.
   *
   * @param dataInput the DataInput stream to read from
   * @return the deserialized value of type T
   */
  def read(dataInput: DataInput): T

  /**
   * Writes a value of type T to the given DataOutput stream.
   *
   * @param value   the value to serialize
   * @param dataOutput the DataOutput stream to write to
   */
  def write(value:T, dataOutput:DataOutput): Unit

  extension (t: T)(using rw: ReadWrite[T]) {

    /**
     * Writes this value to the given DataOutput stream using the implicit ReadWrite instance.
     *
     * @param dataOutput the DataOutput stream to write to
     */
    def writeTo(dataOutput: DataOutput): Unit = {
      rw.write(t, dataOutput)
    }

    /**
     * Serializes the value to a byte array with optional compression.
     *
     * The returned byte array has the following format:
     * - First byte: compression flag (1 if compressed, 0 if uncompressed)
     * - Remaining bytes: the serialized data (either compressed or uncompressed)
     *
     * @return A byte array containing the compression flag followed by the serialized
     *         (and possibly compressed) representation of the value.
     */
    def toBytes: Array[Byte] = {
      var buff = new ByteArrayOutputStream()
      new DataOutputStream(buff) | { out =>
        writeTo(out)
      }
      val data = buff.toByteArray
      buff = new ByteArrayOutputStream()
      new DeflaterOutputStream(buff, new Deflater(Deflater.BEST_COMPRESSION, true) )|{ zip =>
        zip.write(data)
      }
      val zip = buff.toByteArray
      val res = new Array[Byte](zip.length.min(data.length) + 1)
      if(zip.length < data.length){
        res(0) = 1
        System.arraycopy(zip, 0, res, 1, zip.length)
      }else{
        res(0) = 0
        System.arraycopy(data, 0, res, 1, data.length)
      }
      res
    }

  }

}

/**
 * Companion object providing automatic derivation of ReadWrite instances for case classes
 */
object ReadWrite {

  inline given derived[T](using m: Mirror.Of[T]): ReadWrite[T] = {
    lazy val elemInstances = summonAll[Tuple.Map[m.MirroredElemTypes, ReadWrite]]
    inline m match
      case s: Mirror.SumOf[T] => readWriteSum(s, elemInstances)
      case p: Mirror.ProductOf[T] => readWriteProduct(p, elemInstances)
  }

  private def readWriteSum[T](s: Mirror.SumOf[T], instances: => Tuple ): ReadWrite[T] = new ReadWrite[T] {

    override def read(in: DataInput): T = {
      val index = in.readInt()
      val rw = instances(index).asInstanceOf[ReadWrite[T]]
      rw.read(in)
    }

    override def write(t: T, out: DataOutput): Unit = {
      val index = s.ordinal(t)
      out.writeInt(index)

      val rw = instances(index).asInstanceOf[ReadWrite[T]]
      rw.write(t, out)
    }

  }

  private def readWriteProduct[T](p: Mirror.ProductOf[T], instances: => Tuple): ReadWrite[T] = new ReadWrite[T] {

    override def read(in: DataInput): T = {
      var t: Tuple = EmptyTuple
      for(i <- 0 until instances.productArity){
        val rw = instances.productElement(i).asInstanceOf[ReadWrite[Any]]
        val e = rw.read(in)
        t = t :* e
      }
      p.fromProduct(t)
    }

    override def write(t: T, out: DataOutput): Unit = {
      val tp = t.asInstanceOf[Product]
      for(i <- 0 until instances.productArity){
        val rw = instances.productElement(i).asInstanceOf[ReadWrite[Any]]
        rw.write(tp.productElement(i), out)
      }
    }

  }

}
