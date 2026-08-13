package org.wisp.exceptions

import org.wisp.{Actor, Message}

object ProcessingException {

  /**
   * Extractor for [[ProcessingException]]
   */
  def unapply(e: ProcessingException): (Message[?, ?], Actor[?, ?]) = {
    (e.message, e.actor)
  }

}

/**
 * `actor` failed processing `message` with `exception`
 */
class ProcessingException(val message: Message[?, ?], val actor: Actor[?, ?], exception: Throwable)
  extends RuntimeException("Error processing "+message+" in "+actor+": "+exception.getMessage, exception)
