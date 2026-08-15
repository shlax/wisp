package org.wisp.stream.iterator

import scala.concurrent.ExecutionContextExecutor

/**
 * Represents a flow of streaming elements of type `T`.
 * The `StreamFlow` trait provides a mechanism to process elements in a stream through a `callback` function.
 *
 * @tparam T The type of elements in the stream.
 */
trait StreamFlow[T] {

  /**
   * Requests the next element from the stream and invokes the provided `callback` function
   * with a [[Response]] instance. The callback will receive either the next element
   * wrapped in [[Next]] or [[End]] to signify the end of the stream.
   *
   * @param callback A function to process the [[Response]] received from the stream.
   * @param executionContext An implicit `ExecutionContextExecutor` used to execute the callback asynchronously.
   */
  def next(callback: Response[T] => Unit)(using executionContext : ExecutionContextExecutor) : Unit

}
