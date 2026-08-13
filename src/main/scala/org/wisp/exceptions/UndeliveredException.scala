package org.wisp.exceptions

import org.wisp.Message

object UndeliveredException {

  /**
   * Extractor for [[UndeliveredException]]
   */
  def unapply(e: UndeliveredException): Tuple1[Message[?, ?]] = {
    Tuple1(e.message)
  }

}

/**
 *  `message` cannot be delivered
 */
class UndeliveredException(val message: Message[?, ?])
  extends UnsupportedOperationException("Undelivered massage: "+message)
