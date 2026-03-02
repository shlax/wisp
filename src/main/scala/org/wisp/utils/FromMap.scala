package org.wisp.utils

import scala.compiletime.*
import scala.deriving.*

trait FromMap[T] {

  def fromMap(map:Map[String, ?]):T

}

object FromMap {

  inline given derived[T](using m: Mirror.Of[T]): FromMap[T] = {
    val labels = constValueTuple[m.MirroredElemLabels]
    inline m match
      case p: Mirror.ProductOf[T] => fromMapProduct(p, labels)
  }

  private def fromMapProduct[T](p: Mirror.ProductOf[T], labels:Tuple): FromMap[T] = new FromMap[T] {

    override def fromMap(map:Map[String, ?]):T = {
      var t: Tuple = EmptyTuple

      for(i <- 0 until labels.productArity){
        val v = map(labels.productElement(i).toString)
        t = t :* v
      }
      p.fromProduct(t)
    }

  }

}
