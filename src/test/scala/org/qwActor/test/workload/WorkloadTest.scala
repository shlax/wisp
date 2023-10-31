package org.qwActor.test.workload

import org.qwActor.ActorSystem
import org.qwActor.remote.RemoteSystem
import org.qwActor.stream.{Flow, WaitBarrier}
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.CountDownLatch
import scala.util.Using

class WorkloadTest extends AnyFunSuite{

  test("workload"){
    Using(new ActorSystem) { as =>
      val barrier = WaitBarrier[DoWork]()
      val balancer = as.create(c => new Balancer(barrier, c))

      as.create(c => new Worker(balancer, "w1", c))
      as.create(c => new Worker(balancer, "w2", c))

      val cd = new CountDownLatch(10)
      Flow(1 to 10) { f => f.map(i => DoWork(cd, i)).to(barrier) }
      cd.await()

    }
  }

}
