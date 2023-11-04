package org.miniActor.stream.iterator

sealed trait IteratorMessage

/** use with org.miniActor.ActorRef#ask(java.lang.Object) */
case object HasNext extends IteratorMessage

sealed trait IteratorResponseMessage extends IteratorMessage

final case class Next(value:Any) extends IteratorResponseMessage

case object End extends IteratorResponseMessage