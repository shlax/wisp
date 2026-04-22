package org.wisp.stream.iterator

/**
 * Stream operation
 */
sealed trait Operation[+T]

/**
 * Request next element
 */
case object HasNext extends Operation[Nothing]

/**
 * Stream response
 */
sealed trait Response[+T] extends Operation[T]

/**
 * Next element
 */
final case class Next[T](value: T) extends Response[T]

/**
 * End of stream
 */
case object End extends Response[Nothing]
