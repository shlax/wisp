package org.wisp.exceptions

import org.wisp.Message

case class UndeliveredException(message: Message)
  extends Exception("Undelivered massage: "+message)
