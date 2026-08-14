package org.wisp.utils

import org.wisp.ActorSystem
import org.wisp.utils.closeable.*

import scala.concurrent.ExecutionContextExecutor

object extensions {

  extension [T <: ActorSystem](as: T) {

    /**
     * Execute `function` within `ActorSystem` given as ExecutionContextExecutor
     *
     * {{{
     * ActorSystem() || { sys =>
     *   // given ExecutionContextExecutor = sys
     *   ...
     * }
     * }}}
     */
    def || [R](function: ExecutionContextExecutor ?=> T => R): R = {
      as | { a =>
        given ExecutionContextExecutor = as
        function.apply(a)
      }
    }
  }

}
