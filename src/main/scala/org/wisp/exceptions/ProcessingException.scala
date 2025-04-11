package org.wisp.exceptions

import org.wisp.{Actor, Message}

/** `actor` failed processing `message` with `exception` */
case class ProcessingException(message: Message, actor: Actor, exception: Throwable)
  extends Exception("Error processing "+message+" in "+actor+": "+exception.getMessage, exception)
