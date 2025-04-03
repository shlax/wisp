package org.wisp.test.impl

import org.wisp.ActorSystem
import org.wisp.closeable.*

import scala.concurrent.ExecutionContext

object tests {

  extension [T <: ActorSystem](as:T) {
    def || [R](fn: ExecutionContext ?=> T => R ):R = {
      as | { a =>
        given ExecutionContext = as
        fn.apply(a)
      }
    }
  }

}
