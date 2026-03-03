package org.wisp.utils

object conversions {

  def fromMap[T](m: Map[String, Any])(using fm: FromMap[T]): T = {
    val res = fm.fromMap(m)
    res match {
      case p:Product =>
        if(p.productArity != m.size){
          var s = m.keySet
          for(i <- p.productElementNames){
            s -= i
          }
          throw new IllegalArgumentException(s"Unexpected fields: [${s.mkString(", ")}]")
        }
    }
    res
  }

  extension (t: Product) {
    def toMap : Map[String, Any] = {
      val m = Map.newBuilder[String, Any]
      for(i <- 0 until t.productArity){
        m += t.productElementName(i) -> t.productElement(i)
      }
      m.result()
    }
  }

}
