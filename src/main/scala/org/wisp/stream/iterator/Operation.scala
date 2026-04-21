package org.wisp.stream.iterator

/**
 * Stream operation
 */
sealed trait Operation

/**
 * Request next element
 */
case object HasNext extends Operation

/**
 * Stream response
 */
sealed trait Response extends Operation

/**
 * Next element
 */
final case class Next(value: Any) extends Response

/**
 * End of stream
 */
case object End extends Response
