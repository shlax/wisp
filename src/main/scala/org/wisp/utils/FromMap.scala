package org.wisp.utils

import scala.compiletime.*
import scala.deriving.*

trait FromMap[T] {

  def fromMap(map:Map[String, ?]):T

}

object FromMap {

  inline given derived[T](using m: Mirror.Of[T]): FromMap[T] = {
    lazy val labels = constValueTuple[m.MirroredElemLabels].toArray.map(_.toString)
    inline m match
      case p: Mirror.ProductOf[T] => fromMapProduct(p, labels)
  }

  private def fromMapProduct[T](p: Mirror.ProductOf[T], labels: => Array[String]): FromMap[T] = (map: Map[String, ?]) => {
    var t: Tuple = EmptyTuple
    for (i <- labels) {
      val v = map(i)
      t = t :* v
    }
    p.fromProduct(t)
  }

}
