package org.wisp.stream.iterator.message

case class End(exception: Option[Throwable] = None) extends ResponseMessage
