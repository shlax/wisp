package org.wisp.stream.iterator

sealed trait StreamMessage

/** use with org.wisp.ActorRef#ask(java.lang.Object) */
case object HasNext extends StreamMessage

sealed trait StreamResponseMessage extends StreamMessage

final case class Next(value:Any) extends StreamResponseMessage

case object End extends StreamResponseMessage