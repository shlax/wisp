package org.qwActor.test.workload

import org.qwActor.ActorSystem
import org.qwActor.remote.RemoteSystem
import org.qwActor.stream.{WaitBarrier, Flow}
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.CountDownLatch

class WorkloadTest extends AnyFunSuite{

  test("workload"){
    val as = new ActorSystem

    val barrier = WaitBarrier[DoWork]()
    val balancer = as.create( c => new Balancer(barrier, c) )

    as.create( c => new Worker(balancer, "w1", c) )
    as.create( c => new Worker(balancer, "w2", c) )

    val cd = new CountDownLatch(10)
    Flow(1 to 10){ f => f.map( i => DoWork(cd, i) ).to(barrier) }
    cd.await()

    as.close()
  }

}
