package org.wisp.exceptions

import org.wisp.Message

case class UndeliveredException(message: Message)
  extends UnsupportedOperationException("Undelivered massage: "+message)
