package org.wisp.remote.exceptions

object RemoteAskException {

  def unapply(e:RemoteAskException):Tuple1[Any] = {
    Tuple1(e.parameter)
  }

}

class RemoteAskException(val parameter:Any)
  extends UnsupportedOperationException("ask("+parameter+") is not supported")
