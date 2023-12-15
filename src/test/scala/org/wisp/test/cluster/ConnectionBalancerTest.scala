package org.wisp.test.cluster

import org.scalatest.funsuite.AnyFunSuite
import org.wisp.remote.cluster.topology.ConnectionBalancer

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

      val gf = c.groupBy(_.from).map(_._2.size).toSet
      assert( gf.max - gf.min <= 1 )

      val gt = c.groupBy(_.to).map(_._2.size).toSet
      assert(gt.max - gt.min <= 1)

      assert( gf.min == gt.min )
      assert( gf.max == gt.max )
    }

  }

}
