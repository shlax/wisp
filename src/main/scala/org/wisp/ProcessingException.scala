package org.wisp

case class ProcessingException(message: Message, actor: Actor, exception: Throwable)
  extends Exception("error processing "+message+" in "+actor+": "+exception.getMessage)
