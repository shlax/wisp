package org.wisp.io

import java.io.{DataInput, DataOutput}
import scala.compiletime.*
import scala.deriving.*

trait ReadWrite[T] {

  def read(in:DataInput): T

  def write(t:T, out:DataOutput): Unit

}

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
