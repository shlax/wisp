package org.wisp.utils

import org.wisp.ActorSystem
import org.wisp.utils.closeable.*

import scala.concurrent.ExecutionContext

object extensions {

  extension [T <: ActorSystem](as: T) {

    /**
     * Execute `function` with `ActorSystem`
     *
     * {{{
     * ActorSystem() || { sys =>
     *   // given ExecutionContext = sys
     *   ...
     * }
     * }}}
     */
    def ||[R](function: ExecutionContext ?=> T => R): R = {
      as | { a =>
        given ExecutionContext = as
        function.apply(a)
      }
    }
  }

}
