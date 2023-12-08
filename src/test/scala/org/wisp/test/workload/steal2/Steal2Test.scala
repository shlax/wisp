package org.wisp.test.workload.steal2

import org.scalatest.funsuite.AnyFunSuite
import org.wisp.ActorSystem

import scala.util.Using

class Steal2Test extends AnyFunSuite{

  test("steal2") {
    Using(new ActorSystem) { as =>
      val balancer = as.create(c => new TaskBalancer(c))

      val workers = for(i <- 1 to 3) yield {
        val worker = as.create(c => new TaskWorker("W"+i,balancer,c))
        new TaskExecutor(worker)
      }

      // for(w <- workers) w.submit(new Task(w))
      val w = workers.head
      w.submit(new Task(w))

      Thread.sleep(30_000)
      TaskWorker.pull.close()
    }.get
  }

}
