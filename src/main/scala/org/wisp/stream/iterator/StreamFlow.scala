package org.wisp.stream.iterator

import scala.concurrent.ExecutionContextExecutor

trait StreamFlow[T] {

  def next(callback: Response[T] => Unit)(using ec : ExecutionContextExecutor) : Unit

}
