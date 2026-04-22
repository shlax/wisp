package org.wisp.exceptions

import org.wisp.{Actor, LinkCallback}

/** `actor` failed processing `message` with `exception` */
case class ProcessingException(message: LinkCallback[?, ?], actor: Actor[?, ?], exception: Throwable)
  extends Exception("Error processing "+message+" in "+actor+": "+exception.getMessage, exception)
