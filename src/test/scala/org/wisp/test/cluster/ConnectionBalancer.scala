package org.wisp.test.cluster

import scala.collection.mutable

case class Connection[T](from: T, to: T){
  override def toString: String = "Connection("+from+"=>"+to+")"
}

object ConnectionBalancer{

  def apply [T](nodes:Seq[T]) : List[Connection[T]] = {
    var step = 1

    val ret = mutable.ArrayBuffer[Connection[T]]()

    var added = true
    while (added){
      added = false

      var i = 0
      while (i < nodes.size){
        var j = i + step
        if(j >= nodes.size) j -= nodes.size

        if(i != j && j < nodes.size) {
          val a = nodes(i)
          val b = nodes(j)

          if (!ret.exists(c => (c.from == a && c.to == b) || (c.from == b && c.to == a))) {
            ret += Connection(nodes(i), nodes(j))
            added = true
          }
        }

        i += 1
      }

      step += 1
    }

    ret.toList
  }

}
