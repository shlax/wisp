package org.wisp.remote.exceptions

case class UnsupportedAskException(parameter:Any)
  extends UnsupportedOperationException("ask("+parameter+") is not supported")
