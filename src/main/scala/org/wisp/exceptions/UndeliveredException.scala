package org.wisp.exceptions

import org.wisp.Message

/** `message` cannot be delivered */
case class UndeliveredException(message: Message)
  extends UnsupportedOperationException("Undelivered massage: "+message)
