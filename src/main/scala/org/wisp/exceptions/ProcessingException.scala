package org.wisp.exceptions

import org.wisp.{Actor, Message}

case class ProcessingException(message: Message, actor: Actor, exception: Throwable)
  extends Exception("Error processing "+message+" in "+actor+": "+exception.getMessage)
