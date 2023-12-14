package org.wisp.test.cluster

import org.scalatest.funsuite.AnyFunSuite

class ConnectionBalancerTest extends AnyFunSuite{

  test("connectionBalancerTest") {
    for(k <- 2 to 50) {
      val nodes = for (i <- 1 to k) yield i

      val c = ConnectionBalancer(nodes.toList)

      for (i <- nodes; j <- nodes) {
        var cnt = 0
        for (x <- c if (x.from == i && x.to == j) || (x.from == j && x.to == i)) cnt += 1
        if (i == j) {
          assert(cnt == 0)
        } else {
          assert(cnt == 1)
        }
      }

      val g = c.groupBy(_.from).map(_._2.size).toSet
      assert( g.max - g.min <= 1 )
    }

  }

}
