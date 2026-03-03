package org.wisp.utils

object conversions {

  def fromMap[T](m: Map[String, ?])(using fm: FromMap[T]): T = {
    val res = fm.fromMap(m)
    res match {
      case p:Product =>
        if(p.productArity != m.size){
          var s = m.keySet
          for(i <- p.productElementNames){
            s -= i
          }
          throw new IllegalArgumentException(s"Unexpected fields: ${s.mkString(", ")}")
        }
    }
    res
  }

}
