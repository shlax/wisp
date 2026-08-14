package org.wisp.stream.iterator

/**
 * Stream response
 */
sealed trait Response[+T]

/**
 * Next element
 */
final case class Next[T](value: T) extends Response[T]

/**
 * End of stream
 */
case object End extends Response[Nothing]
