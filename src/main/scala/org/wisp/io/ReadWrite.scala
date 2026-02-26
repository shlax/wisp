package org.wisp.io

import java.io.{ObjectInputStream, ObjectOutputStream}
import scala.compiletime.*
import scala.deriving.*

trait ReadWrite[T] {

  def read(in:ObjectInputStream): T

  def write(t:T, out:ObjectOutputStream): Unit

}

object ReadWrite {

  inline def derived[T](using m: Mirror.Of[T]): ReadWrite[T] = {
    lazy val elemInstances = summonAll[Tuple.Map[m.MirroredElemTypes, ReadWrite]].toList.asInstanceOf[List[ReadWrite[Any]]]
    m match
      case s: Mirror.SumOf[T] => readWriteSum(s, elemInstances)
      case p: Mirror.ProductOf[T] => readWriteProduct(p, elemInstances)
  }

  private def readWriteSum[T](s: Mirror.SumOf[T], instances: => List[ReadWrite[Any]] ): ReadWrite[T] = new ReadWrite[T] {

    override def read(in: ObjectInputStream): T = {
      val index = in.readInt()
      instances(index).read(in).asInstanceOf[T]
    }

    override def write(t: T, out: ObjectOutputStream): Unit = {
      val index = s.ordinal(t)
      out.writeInt(index)
      instances(index).write(t, out)
    }

  }

  private def readWriteProduct[T](p: Mirror.ProductOf[T], instances: => List[ReadWrite[Any]]): ReadWrite[T] = new ReadWrite[T] {

    override def read(in: ObjectInputStream): T = {
      val l = instances.map( _.read(in) )
      val t = l.foldRight[Tuple](EmptyTuple)( _ *: _ )
      p.fromProduct(t)
    }

    override def write(t: T, out: ObjectOutputStream): Unit = {
      val tp = t.asInstanceOf[Product]
      tp.productIterator.zip(instances).foreach{ (e, i) =>
        i.write(e, out)
      }
    }

  }

}
