package org.wisp.remote.cluster.topology

import scala.collection.mutable

object ConnectionBalancer{

  def apply [T](nodes:Seq[T]) : List[Connection[T]] = {
    val ret = mutable.ArrayBuffer[Connection[T]]()

    var step = 1
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