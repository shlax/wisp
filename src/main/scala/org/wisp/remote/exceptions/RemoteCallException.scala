package org.wisp.remote.exceptions

object RemoteCallException {

  def unapply(e:RemoteCallException):Tuple1[Any] = {
    Tuple1(e.parameter)
  }

}

class RemoteCallException(val parameter:Any)
  extends UnsupportedOperationException("call("+parameter+") is not supported")
