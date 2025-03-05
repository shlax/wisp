package org.wisp.test

import org.wisp.ActorSystem
import org.wisp.using.*

import scala.concurrent.ExecutionContext

object testSystem {

  extension (as:ActorSystem) {
    def || [R](fn: ExecutionContext ?=> ActorSystem => R ):R = {
      as | { a =>
        given ExecutionContext = as
        fn.apply(a)
      }
    }
  }

}
