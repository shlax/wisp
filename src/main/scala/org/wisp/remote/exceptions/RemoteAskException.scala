package org.wisp.remote.exceptions

case class RemoteAskException(parameter:Any)
  extends UnsupportedOperationException("ask("+parameter+") is not supported")
