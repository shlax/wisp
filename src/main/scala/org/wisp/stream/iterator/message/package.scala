package org.wisp.stream.iterator

package object message {

  sealed trait IteratorMessage extends Serializable

  case object HasNext extends IteratorMessage

  sealed trait ResponseMessage extends IteratorMessage

  final case class Next(value: Any) extends ResponseMessage

  case object End extends ResponseMessage
}
