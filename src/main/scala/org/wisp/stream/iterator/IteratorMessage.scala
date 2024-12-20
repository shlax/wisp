package org.wisp.stream.iterator

sealed trait IteratorMessage extends Serializable

object HasNext extends IteratorMessage

sealed trait IteratorNextMessage extends IteratorMessage

object Next {
  def unapply(v:Next): Tuple1[Any] = Tuple1(v.value)
}

final class Next(val value:Any) extends IteratorNextMessage

object End extends IteratorNextMessage
