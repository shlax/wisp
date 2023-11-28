package org.wisp.test.workload.steal

import org.scalatest.funsuite.AnyFunSuite
import org.wisp.ActorSystem

import scala.util.Using

class StealTest extends AnyFunSuite{

  test("steal"){
    Using(new ActorSystem) { as =>
      val b = as.create(c => new TaskBalancer(c))
      b << new Task

      Thread.sleep(30_000)
      TaskWorker.pull.close()
    }.get
  }

}
