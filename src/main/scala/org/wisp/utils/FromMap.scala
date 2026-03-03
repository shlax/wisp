package org.wisp.utils

import scala.compiletime.*
import scala.deriving.*

/**
 * conversion from Map to case class
 *
 * usage:
 * {{{
 *   case class IdName(id:Int, name:String) derives FromMap
 *
 *   def fromMap[T](m: Map[String, ?])(using fm: FromMap[T]): T = {
 *     fm.fromMap(m)
 *   }
 *
 *   val map = Map("id" -> 1, "name" -> "test")
 *   val idName = fromMap[IdName](map)
 * }}}
 * */
trait FromMap[T] {

  def fromMap(map:Map[String, Any]):T

}

object FromMap {

  inline given derived[T](using m: Mirror.Of[T]): FromMap[T] = {
    lazy val labels = constValueTuple[m.MirroredElemLabels].toList.map(_.toString)
    lazy val types = elemTypes[m.MirroredElemTypes]
    inline m match
      case p: Mirror.ProductOf[T] => fromMapProduct(p, labels, types)
  }

  private inline def elemTypes[A <: Tuple]: List[(String, Any) => Any] = {
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (head *: tail) =>
        val headElement = {
          (nm:String, x: Any) =>
            try {
              x.asInstanceOf[head]
            }catch {
              case e:ClassCastException =>
                throw new RuntimeException(s"Cannot cast $nm = $x", e)
            }
        }
        val tailElements = elemTypes[tail]
        headElement :: tailElements
    }
  }

  private def fromMapProduct[T](p: Mirror.ProductOf[T], labels: => List[String], types: => List[(String, Any) => Any]): FromMap[T] = (map: Map[String, ?]) => {
    var t: Tuple = EmptyTuple
    for (i <- labels.zip(types)) {
      val v = i._2.apply(i._1, map(i._1) )
      t = t :* v
    }
    p.fromProduct(t)
  }

}
