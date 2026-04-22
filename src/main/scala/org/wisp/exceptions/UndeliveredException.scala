package org.wisp.exceptions

import org.wisp.LinkCallback

/** `message` cannot be delivered */
case class UndeliveredException(message: LinkCallback[?, ?])
  extends UnsupportedOperationException("Undelivered massage: "+message)
