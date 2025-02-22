package org.wisp

case class UndeliveredException(message: Message)
  extends Exception("undelivered massage "+message)
